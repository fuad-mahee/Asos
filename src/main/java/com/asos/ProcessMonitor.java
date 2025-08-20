package com.asos;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ProcessMonitor implements Runnable {
    private final ProgressListener listener;
    private Set<Long> previousPids = new HashSet<>();
    private boolean running = true;
    private final long pollIntervalMillis;

    public interface ProgressListener {
        void onProcessEvent(String message);
    }

    public ProcessMonitor(ProgressListener listener, long pollIntervalMillis) {
        this.listener = listener;
        this.pollIntervalMillis = pollIntervalMillis;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            Set<Long> currentPids = ProcessHandle.allProcesses()
                    .map(ProcessHandle::pid)
                    .collect(Collectors.toSet());

            // Detect started processes
            Set<Long> started = new HashSet<>(currentPids);
            started.removeAll(previousPids);
            for (Long pid : started) {
                ProcessHandle ph = ProcessHandle.of(pid).orElse(null);
                if (ph != null && ph.info().command().isPresent()) {
                    String cmd = ph.info().command().get();
                    String appName = extractAppName(cmd);
                    listener.onProcessEvent("Started: PID=" + pid + ", App=" + appName + ", CMD=" + cmd + ", Time=" + Instant.now());
                }
            }

            // Detect stopped processes
            Set<Long> stopped = new HashSet<>(previousPids);
            stopped.removeAll(currentPids);
            for (Long pid : stopped) {
                listener.onProcessEvent("Stopped: PID=" + pid + ", Time=" + Instant.now());
            }

            previousPids = currentPids;
            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Extracts a user-friendly app name from the command path
    private static String extractAppName(String cmdPath) {
        if (cmdPath == null || cmdPath.isEmpty()) return "Unknown";
        // Try to extract the .app name if present (macOS)
        int appIdx = cmdPath.lastIndexOf(".app/");
        if (appIdx > 0) {
            int start = cmdPath.lastIndexOf('/', appIdx - 1) + 1;
            return cmdPath.substring(start, appIdx + 4); // e.g., Firefox.app
        }
        // Otherwise, just use the last part of the path
        int slashIdx = cmdPath.lastIndexOf('/');
        if (slashIdx >= 0 && slashIdx < cmdPath.length() - 1) {
            return cmdPath.substring(slashIdx + 1);
        }
        return cmdPath;
    }
}
