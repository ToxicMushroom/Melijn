package me.melijn.melijnbot.internals.threading

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


object TaskManager {

    private val threadFactory = { name: String ->
        ThreadFactoryBuilder().setNameFormat("[$name-Pool-%d]").build()
    }

    val executorService: ExecutorService = Executors.newCachedThreadPool(threadFactory.invoke("Task"))
    val dispatcher = executorService.asCoroutineDispatcher()
    val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))

    fun async(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        Task {
            block.invoke(this)
        }.run()
    }

    inline fun asyncInline(crossinline block: CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        TaskInline {
            block.invoke(this)
        }.run()
    }

    inline fun asyncAfter(afterMillis: Long, crossinline func: () -> Unit) {
        scheduledExecutorService.schedule(TaskInline { func() }, afterMillis, TimeUnit.MILLISECONDS)
    }
}
