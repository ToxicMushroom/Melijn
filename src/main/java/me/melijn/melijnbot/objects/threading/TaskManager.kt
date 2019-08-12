package me.melijn.melijnbot.objects.threading

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class TaskManager {

    private val threadFactory = { name: String -> ThreadFactoryBuilder().setNameFormat("[$name-Pool-%d] ").build() }
    private val executorService = Executors.newCachedThreadPool(threadFactory.invoke("Task"))
    private val scheduledExecutorService = Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))

    fun scheduleRepeating(runnable: Runnable, periodMillis: Long) {
        scheduledExecutorService.scheduleAtFixedRate(Task(runnable), 0, periodMillis, TimeUnit.MILLISECONDS)
    }

    fun scheduleRepeating(runnable: Runnable, afterMillis: Long, periodMillis: Long) {
        scheduledExecutorService.scheduleAtFixedRate(Task(runnable), afterMillis, periodMillis, TimeUnit.MILLISECONDS)
    }

    fun async(func: () -> Unit) {
        executorService.submit(Task(Runnable(func)))
    }

    fun asyncAfter(afterMillis: Long, func: () -> Unit) {
        scheduledExecutorService.schedule(Task(Runnable(func)), afterMillis, TimeUnit.MILLISECONDS)
    }

    fun getExecutorService(): ExecutorService {
        return executorService
    }

    fun getScheduledExecutorService(): ScheduledExecutorService {
        return scheduledExecutorService
    }
}