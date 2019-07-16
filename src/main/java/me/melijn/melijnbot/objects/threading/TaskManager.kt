package me.melijn.melijnbot.objects.threading

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.melijn.melijnbot.objects.utils.MessageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class TaskManager(val messageUtils: MessageUtils) {

    private val threadFactory = { name: String -> ThreadFactoryBuilder().setNameFormat("[$name-Pool-%d] ").build() }
    private val executorService = Executors.newCachedThreadPool(threadFactory.invoke("Task"))
    private val scheduledExecutorService = Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))

    fun scheduleRepeating(runnable: Runnable, periodMillis: Long) {
        scheduledExecutorService.scheduleAtFixedRate(Task(messageUtils, runnable), 0, periodMillis, TimeUnit.MILLISECONDS)
    }

    fun scheduleRepeating(runnable: Runnable, afterMillis: Long, periodMillis: Long) {
        scheduledExecutorService.scheduleAtFixedRate(Task(messageUtils, runnable), afterMillis, periodMillis, TimeUnit.MILLISECONDS)
    }

    fun async(runnable: Runnable) {
        executorService.submit(Task(messageUtils, runnable))
    }

    fun asyncAfter(runnable: Runnable, afterMillis: Long) {
        scheduledExecutorService.schedule(Task(messageUtils, runnable), afterMillis, TimeUnit.MILLISECONDS)
    }

    fun getExecutorService(): ExecutorService {
        return executorService
    }

    fun getScheduledExecutorService(): ScheduledExecutorService {
        return scheduledExecutorService
    }
}