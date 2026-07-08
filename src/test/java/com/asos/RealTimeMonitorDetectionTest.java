package com.asos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test of the teaching mode detection pipeline: a real file is
 * created on disk and must be picked up by the FileSystemWatcher ->
 * RealTimeMonitor -> ExpectedAction matching chain.
 *
 * Also verifies that monitoring still works after a stop/start cycle,
 * which is what happens when the learner finishes one course and starts
 * another (this was broken before: the closed WatchService was reused).
 */
class RealTimeMonitorDetectionTest {

    private static final int DETECTION_TIMEOUT_SECONDS = 20;

    private final Path testFile1 = Paths.get(System.getProperty("user.dir"), "AsosDetectionProbe1.java");
    private final Path testFile2 = Paths.get(System.getProperty("user.dir"), "AsosDetectionProbe2.java");
    private final Path pythonProbe = Paths.get(System.getProperty("user.dir"), "AsosRunProbe.py");
    private RealTimeMonitor monitor;

    @AfterEach
    void cleanup() throws Exception {
        if (monitor != null) {
            monitor.stopMonitoring();
        }
        Files.deleteIfExists(testFile1);
        Files.deleteIfExists(testFile2);
        Files.deleteIfExists(pythonProbe);
    }

    @Test
    @DisplayName("File creation is detected, including after a stop/restart cycle")
    void detectsFileCreationAndSurvivesRestart() throws Exception {
        monitor = new RealTimeMonitor();

        // --- Session 1: detect a newly created file ---
        CountDownLatch detected1 = new CountDownLatch(1);
        monitor.setOnActionDetected(action -> detected1.countDown());

        LearningChunk.ExpectedAction action1 = new LearningChunk.ExpectedAction(
                LearningChunk.ExpectedAction.ActionType.FILE_CREATED,
                "AsosDetectionProbe1.java",
                ".*AsosDetectionProbe1\\.java$");

        monitor.startMonitoring(List.of(action1));
        Thread.sleep(1000); // give the watcher thread time to start

        Files.writeString(testFile1, "public class AsosDetectionProbe1 {}");

        assertTrue(detected1.await(DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "File creation was not detected within " + DETECTION_TIMEOUT_SECONDS + "s");
        assertTrue(monitor.areAllActionsCompleted(), "Chunk should be complete after detection");

        // --- Stop, then start a second session (new course scenario) ---
        monitor.stopMonitoring();

        CountDownLatch detected2 = new CountDownLatch(1);
        monitor.setOnActionDetected(action -> detected2.countDown());

        LearningChunk.ExpectedAction action2 = new LearningChunk.ExpectedAction(
                LearningChunk.ExpectedAction.ActionType.FILE_CREATED,
                "AsosDetectionProbe2.java",
                ".*AsosDetectionProbe2\\.java$");

        monitor.startMonitoring(List.of(action2));
        Thread.sleep(1000);

        Files.writeString(testFile2, "public class AsosDetectionProbe2 {}");

        assertTrue(detected2.await(DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "Detection after a stop/restart cycle failed - monitoring is not restartable");
    }

    @Test
    @DisplayName("RUN_SUCCESS verifies a Python program's output (the python tutorial flow)")
    void verifiesPythonProgramOutput() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(isPythonAvailable(),
                "python is not on PATH - skipping");

        monitor = new RealTimeMonitor();

        CountDownLatch detected = new CountDownLatch(1);
        monitor.setOnActionDetected(action -> detected.countDown());

        Files.writeString(pythonProbe, "print(\"Hello, World!\")");

        LearningChunk.ExpectedAction action = new LearningChunk.ExpectedAction(
                LearningChunk.ExpectedAction.ActionType.RUN_SUCCESS,
                "AsosRunProbe.py",
                "Hello.*World");

        monitor.startMonitoring(List.of(action));

        assertTrue(detected.await(30, TimeUnit.SECONDS),
                "Python program output was not verified - RUN_SUCCESS is broken for .py targets");
    }

    private boolean isPythonAvailable() {
        try {
            Process process = new ProcessBuilder("python", "--version")
                    .redirectErrorStream(true).start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    @DisplayName("CODE_CONTAINS matches file content on modification")
    void detectsCodeContent() throws Exception {
        monitor = new RealTimeMonitor();

        CountDownLatch detected = new CountDownLatch(1);
        monitor.setOnActionDetected(action -> detected.countDown());

        LearningChunk.ExpectedAction action = new LearningChunk.ExpectedAction(
                LearningChunk.ExpectedAction.ActionType.CODE_CONTAINS,
                "AsosDetectionProbe1.java",
                "public class AsosDetectionProbe1");

        // Create the file first, then start monitoring and modify it
        Files.writeString(testFile1, "// empty");
        monitor.startMonitoring(List.of(action));
        Thread.sleep(1000);

        Files.writeString(testFile1, "public class AsosDetectionProbe1 { }");

        assertTrue(detected.await(DETECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "CODE_CONTAINS was not detected on file modification");
    }
}
