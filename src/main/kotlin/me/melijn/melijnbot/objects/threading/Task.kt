package me.melijn.melijnbot.objects.threading

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.objects.utils.sendInGuild


class Task(private val func: suspend () -> Unit) : Runnable {


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