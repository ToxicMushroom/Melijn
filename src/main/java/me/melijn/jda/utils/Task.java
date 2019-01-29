package me.melijn.jda.utils;

import java.time.LocalDate;
import java.util.function.Predicate;

public final class Task implements Runnable {

    private final Runnable runnable;
    private final boolean repeating;
    private final long period;
    private final Predicate<LocalDate> predicate;
    private boolean stop = false;
    private final long initialDelay;
    private final MessageHelper messageHelper;

    public Task(final MessageHelper messageHelper, final Runnable runnable, final long initialDelay, final long period, final Predicate<LocalDate> predicate) {
        this.runnable = runnable;
        this.repeating = true;
        this.initialDelay = initialDelay;
        this.period = period;
        this.predicate = predicate;
        this.messageHelper = messageHelper;
    }

    public Task(final MessageHelper messageHelper, final Runnable runnable, final long initialDelay, final long period) {
        this(messageHelper, runnable, initialDelay, period, null);
    }

    public Task(final MessageHelper messageHelper, final Runnable runnable, final long initialDelay) {
        this.initialDelay = initialDelay;
        this.runnable = runnable;
        this.repeating = false;
        this.period = -1;
        this.predicate = null;
        this.messageHelper = messageHelper;
    }

    public Task(final MessageHelper messageHelper, final Runnable runnable) {
        this.initialDelay = -1;
        this.runnable = runnable;
        this.repeating = false;
        this.period = -1;
        this.predicate = null;
        this.messageHelper = messageHelper;
    }

    public void stop() {
        stop = true;
    }

    @Override
    public void run() {
        initWait();
        try {
            if (!this.repeating)
                runnable.run();

            while (repeating && !stop) {
                if (predicate != null && !predicate.test(LocalDate.now()))
                    waitNow();
                runnable.run();
                waitNow();
            }
        } catch (Exception e) {
            e.printStackTrace();
            messageHelper.printException(Thread.currentThread(), e, null, null);
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