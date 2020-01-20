package me.melijn.melijnbot.objects.threading

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class TaskManager {

    private val threadFactory = { name: String ->
        ThreadFactoryBuilder().setNameFormat("[$name-Pool-%d] ").build()
    }

    val executorService: ExecutorService = Executors.newCachedThreadPool(threadFactory.invoke("Task"))
    val dispatcher = executorService.asCoroutineDispatcher()
    val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))

    fun async(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        val task = Task(Runnable {
            CoroutineScope(dispatcher).launch {
                block.invoke(this)
            }
        })
        task.run()
    }


    fun asyncAfter(afterMillis: Long, func: () -> Unit) {
        scheduledExecutorService.schedule(Task(Runnable(func)), afterMillis, TimeUnit.MILLISECONDS)
    }
}