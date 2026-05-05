package project.shared;

import javafx.application.Platform;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public final class CrashSimulationManager {
    private static final Random RANDOM = new Random();
    private static Timer randomCrashTimer;
    private static boolean randomCrashEnabled = false;

    private CrashSimulationManager() {
    }

    public static synchronized boolean isRandomCrashEnabled() {
        return randomCrashEnabled;
    }

    public static synchronized void enableRandomCrash(int minSeconds, int maxSeconds, Runnable beforeCrash) {
        disableRandomCrash();
        randomCrashEnabled = true;
        randomCrashTimer = new Timer(true);
        scheduleNextRandomCrash(minSeconds, maxSeconds, beforeCrash);
    }

    public static synchronized void disableRandomCrash() {
        randomCrashEnabled = false;
        if (randomCrashTimer != null) {
            randomCrashTimer.cancel();
            randomCrashTimer = null;
        }
    }

    private static synchronized void scheduleNextRandomCrash(int minSeconds, int maxSeconds, Runnable beforeCrash) {
        if (!randomCrashEnabled || randomCrashTimer == null) {
            return;
        }
        int min = Math.max(1, minSeconds);
        int max = Math.max(min, maxSeconds);
        int delaySeconds = min + RANDOM.nextInt(max - min + 1);
        randomCrashTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                triggerCrashNow(beforeCrash);
            }
        }, delaySeconds * 1000L);
    }

    public static void triggerCrashNow(Runnable beforeCrash) {
        try {
            if (beforeCrash != null) {
                beforeCrash.run();
            }
        } finally {
            Platform.exit();
            System.exit(137);
        }
    }
}
