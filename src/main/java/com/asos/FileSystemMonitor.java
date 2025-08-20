package com.asos;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;

public class FileSystemMonitor implements Runnable {
    private final Path pathToWatch;
    private final ProgressListener listener;

    public interface ProgressListener {
        void onProgress(String message);
    }

    public FileSystemMonitor(Path pathToWatch, ProgressListener listener) {
        this.pathToWatch = pathToWatch;
        this.listener = listener;
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            pathToWatch.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take(); // blocks until an event occurs
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path changed = pathToWatch.resolve((Path) event.context());
                    listener.onProgress("Event: " + kind.name() + " on " + changed);
                    // Here, add logic to check if the event matches the current instruction step
                }
                key.reset();
            }
        } catch (IOException | InterruptedException e) {
            listener.onProgress("Monitoring stopped: " + e.getMessage());
        }
    }
}
