package com.asos;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        // File system monitor
        FileSystemMonitor.ProgressListener fsListener = message -> {
            System.out.println("[Asos?][FS] " + message);
            // Here, update your GUI or agent bubble with feedback
        };
        FileSystemMonitor fsMonitor = new FileSystemMonitor(
            Paths.get(System.getProperty("user.home"), "Desktop"), // Example: monitor Desktop
            fsListener
        );
        new Thread(fsMonitor).start();

        // Process monitor
        ProcessMonitor.ProgressListener procListener = message -> {
            System.out.println("[Asos?][PROC] " + message);
            // Here, update your GUI or agent bubble with feedback
        };
        ProcessMonitor procMonitor = new ProcessMonitor(procListener, 2000); // poll every 2 seconds
        new Thread(procMonitor).start();

        // ...rest of your app (GUI, agent, etc.)
    }
}
