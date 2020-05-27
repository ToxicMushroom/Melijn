package me.melijn.melijnbot.objects.threading

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.objects.utils.sendInGuild


class Task(private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild()
        }
    }
}

class RunnableTask(private val func: suspend () -> Unit) : Runnable {

    override fun run() {
        runBlocking {
            try {
                func()
            } catch (e: Throwable) {
                e.printStackTrace()
                e.sendInGuild()
            }
        }
    }
}

class TaskInline(private inline val func: () -> Unit) : Runnable {

    override fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild()
        }
    }
}

@FunctionalInterface
interface KTRunnable {
    suspend fun run()
}