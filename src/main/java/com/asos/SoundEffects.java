package com.asos;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Soft synthesized sound cues (no audio files needed - tones are generated
 * on the fly). Respects the sounds on/off setting. All playback happens on
 * short-lived daemon threads and failures are silent: sound must never
 * break the app.
 */
public final class SoundEffects {

    private static final int SAMPLE_RATE = 44100;
    private static final int VOLUME = 42; // out of 127 - deliberately gentle

    private SoundEffects() {
    }

    /** Two rising notes: step completed / achievement unlocked. */
    public static void playSuccess() {
        playTones(new double[][]{{660, 110}, {880, 150}});
    }

    /** Two falling notes: a mistake was detected. */
    public static void playError() {
        playTones(new double[][]{{330, 100}, {230, 170}});
    }

    /** Single mid note: a hint appeared. */
    public static void playHint() {
        playTones(new double[][]{{520, 150}});
    }

    /**
     * Play a sequence of {frequencyHz, durationMs} tones asynchronously.
     */
    private static void playTones(double[][] notes) {
        if (!AppSettings.isSoundEnabled()) {
            return;
        }
        Thread thread = new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                    line.open(format);
                    line.start();
                    for (double[] note : notes) {
                        byte[] samples = synthesize(note[0], (int) note[1]);
                        line.write(samples, 0, samples.length);
                    }
                    line.drain();
                }
            } catch (Exception ignored) {
                // No audio device / line busy - never let sound break the app
            }
        }, "sound-cue");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Generate a sine tone with a short attack/release envelope so the
     * start and end don't click.
     */
    private static byte[] synthesize(double frequencyHz, int durationMs) {
        int length = SAMPLE_RATE * durationMs / 1000;
        byte[] samples = new byte[length];
        double attackSamples = SAMPLE_RATE * 0.012;
        double releaseSamples = SAMPLE_RATE * 0.030;
        for (int i = 0; i < length; i++) {
            double envelope = Math.min(1.0,
                    Math.min(i / attackSamples, (length - i) / releaseSamples));
            samples[i] = (byte) (Math.sin(2 * Math.PI * frequencyHz * i / SAMPLE_RATE)
                    * VOLUME * envelope);
        }
        return samples;
    }
}
