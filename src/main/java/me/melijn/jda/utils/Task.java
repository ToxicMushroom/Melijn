package me.melijn.jda.utils;

public final class Task implements Runnable {

    private final Runnable runnable;
    private final MessageHelper messageHelper;

    public Task(final MessageHelper messageHelper, final Runnable runnable) {
        this.runnable = runnable;
        this.messageHelper = messageHelper;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace();
            messageHelper.printException(Thread.currentThread(), e, null, null);
        }
    }
}