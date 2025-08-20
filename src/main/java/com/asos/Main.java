package com.asos;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        FileSystemMonitor.ProgressListener fsListener = message -> {
            System.out.println("[Asos?][FS] " + message);
        };
        FileSystemMonitor fsMonitor = new FileSystemMonitor(
            Paths.get(System.getProperty("user.home"), "Desktop"),
            fsListener
        );
        new Thread(fsMonitor).start();

        ProcessMonitor.ProgressListener procListener = message -> {
            System.out.println("[Asos?][PROC] " + message);
        };
        ProcessMonitor procMonitor = new ProcessMonitor(procListener, 2000);
        new Thread(procMonitor).start();
    }
}
