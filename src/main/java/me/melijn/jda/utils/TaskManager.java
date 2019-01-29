package me.melijn.jda.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.function.Predicate;

public class TaskManager {

    private final MessageHelper messageHelper;
    private final Function<String, ThreadFactory> threadFactory = name -> new ThreadFactoryBuilder().setNameFormat("[" + name + "-Pool-%d] ").build();
    private final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory.apply("Task"));

    public TaskManager(MessageHelper messageHelper) {
        this.messageHelper = messageHelper;
    }

    public void scheduleRepeating(final Runnable runnable, final long period) {
        executorService.submit(new Task(messageHelper, runnable, 0, period));
    }

    public void scheduleRepeating(final Runnable runnable, final long initialDelay, final long period) {
        executorService.submit(new Task(messageHelper, runnable, initialDelay, period));
    }

    public void async(final Runnable runnable) {
        executorService.submit(new Task(messageHelper, runnable));
    }

    public void async(final Runnable runnable, final long after) {
        executorService.submit(new Task(messageHelper, runnable, after));
    }

    public void scheduleRepeatingWhen(final Runnable runnable, final long period, final Predicate<LocalDate> dateAccessor) {
        executorService.submit(new Task(messageHelper, runnable, -1, period, dateAccessor));
    }

    public void scheduleRepeatingWhen(final Runnable runnable, final long initialDelay, final long period, final Predicate<LocalDate> dateAccessor) {
        executorService.submit(new Task(messageHelper, runnable, initialDelay, period, dateAccessor));
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }
}
