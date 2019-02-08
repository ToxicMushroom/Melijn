package me.melijn.jda.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.LocalDate;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class TaskManager {

    private final MessageHelper messageHelper;
    private final Function<String, ThreadFactory> threadFactory = name -> new ThreadFactoryBuilder().setNameFormat("[" + name + "-Pool-%d] ").build();
    private final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory.apply("Task"));
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10, threadFactory.apply("Rep"));

    public TaskManager(MessageHelper messageHelper) {
        this.messageHelper = messageHelper;
    }

    public void scheduleRepeating(final Runnable runnable, final long period) {
        scheduledExecutorService.scheduleAtFixedRate(new Task(messageHelper, runnable), 0, period, TimeUnit.MILLISECONDS);
    }

    public void scheduleRepeating(final Runnable runnable, final long initialDelay, final long period) {
        scheduledExecutorService.scheduleAtFixedRate(new Task(messageHelper, runnable), initialDelay, period, TimeUnit.MILLISECONDS);
    }

    public void async(final Runnable runnable) {
        executorService.submit(new Task(messageHelper, runnable));
    }

    public void async(final Runnable runnable, final long after) {
        scheduledExecutorService.schedule(new Task(messageHelper, runnable), after, TimeUnit.MILLISECONDS);
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
}
