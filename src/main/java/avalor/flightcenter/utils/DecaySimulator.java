package avalor.flightcenter.utils;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility responsible for running a periodic flight simulation task.
 * Provides static lifecycle methods to start and stop a 2-second timer.
 */
public final class DecaySimulator {

    // Prevent instantiation
    private DecaySimulator() {}

    // Single-threaded scheduler for periodic task
    private static volatile ScheduledExecutorService flightSimScheduler;
    private static volatile ScheduledFuture<?> flightSimScheduledTask;
    private static final Object flightSimLock = new Object();
    private static final AtomicBoolean isFlightRunning = new AtomicBoolean(false);


    /**
     * Starts the flight simulator timer with a configurable period in miliseconds.
     */
    public static void start(long periodMiliSeconds, Runnable runnable) {

        if ((periodMiliSeconds <= 0) || (runnable == null)) {
            throw new IllegalArgumentException("Invalid data to start the flight simulator timer with");
        }

        if (isFlightRunning.get()) {
            return; // already running
        }
        synchronized (flightSimLock) {
            if (isFlightRunning.get()) {
                return;
            }
            flightSimScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "FlightSimulatorTimer");
                t.setDaemon(true);
                return t;
            });

            flightSimScheduledTask = flightSimScheduler.scheduleAtFixedRate(runnable, 0, periodMiliSeconds, TimeUnit.MILLISECONDS);
            isFlightRunning.set(true);
        }
    }

    /**
     * Stops the flight simulator timer if running and releases resources.
     */
    public static void stop() {
        if (!isFlightRunning.get()) {
            return; // not running
        }
        synchronized (flightSimLock) {
            if (!isFlightRunning.get()) {
                return;
            }
            try {
                if (flightSimScheduledTask != null) {
                    flightSimScheduledTask.cancel(false);
                }
            } finally {
                flightSimScheduledTask = null;
            }
            ScheduledExecutorService toShutdown = flightSimScheduler;
            flightSimScheduler = null;
            isFlightRunning.set(false);
            if (Objects.nonNull(toShutdown)) {
                toShutdown.shutdown();
                try {
                    if (!toShutdown.awaitTermination(2, TimeUnit.SECONDS)) {
                        toShutdown.shutdownNow();
                    }
                } catch (InterruptedException ie) {
                    toShutdown.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
