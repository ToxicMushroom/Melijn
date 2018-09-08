package me.melijn.jda.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TaskScheduler implements Runnable {

    private static final Function<String, ThreadFactory> FACTORY = name -> new ThreadFactoryBuilder().setNameFormat("[" + name + "-Pool-%d] ").build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(FACTORY.apply("TaskScheduler"));

    public static void scheduleRepeating(final Runnable runnable, final long period) {
        EXECUTOR_SERVICE.submit(new TaskScheduler(runnable, period));
    }

    public static void scheduleRepeating(final Runnable runnable, final long initialDelay, final long period) {
        EXECUTOR_SERVICE.submit(new TaskScheduler(runnable, initialDelay, period));
    }

    public static void async(final Runnable runnable) {
        EXECUTOR_SERVICE.submit(new TaskScheduler(runnable));
    }

    public static void async(final Runnable runnable, final long after) {
        EXECUTOR_SERVICE.submit(new TaskScheduler(runnable, after));
    }

    public static void scheduleRepeatingWhen(final Runnable runnable, final long period, final Predicate<LocalDate> dateAccessor) {
        EXECUTOR_SERVICE.submit(new TaskScheduler(runnable, -1, period, dateAccessor));
    }

    public static void scheduleRepeatingWhen(final Runnable runnable, final long initialDelay, final long period, final Predicate<LocalDate> dateAccessor) {
        EXECUTOR_SERVICE.submit(new TaskScheduler(runnable, initialDelay, period, dateAccessor));
    }

    private final Runnable runnable;
    private final boolean repeating;
    private final long period;
    private final Predicate<LocalDate> predicate;
    private boolean stop = false;
    private final long initialDelay;

    private TaskScheduler(final Runnable runnable, final long initialDelay, final long period, final Predicate<LocalDate> predicate) {
        this.runnable = runnable;
        this.repeating = true;
        this.initialDelay = initialDelay;
        this.period = period;
        this.predicate = predicate;
    }

    private TaskScheduler(final Runnable runnable, final long initialDelay, final long period) {
        this(runnable, initialDelay, period, null);
    }

    private TaskScheduler(final Runnable runnable, final long initialDelay) {
        this.initialDelay = initialDelay;
        this.runnable = runnable;
        this.repeating = false;
        this.period = -1;
        this.predicate = null;
    }

    private TaskScheduler(final Runnable runnable) {
        this.initialDelay = -1;
        this.runnable = runnable;
        this.repeating = false;
        this.period = -1;
        this.predicate = null;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        initWait();
        if (!this.repeating)
            runnable.run();

        while (repeating && !stop) {
            if (predicate != null)
                if (!predicate.test(LocalDate.now()))
                    waitNow();
            runnable.run();
            waitNow();
        }
    }

    private void initWait() {
        if (initialDelay > 0) {
            try {
                Thread.sleep(initialDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void waitNow() {
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}