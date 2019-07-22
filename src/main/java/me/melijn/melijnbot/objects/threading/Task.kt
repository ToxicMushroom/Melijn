package me.melijn.melijnbot.objects.threading

import me.melijn.melijnbot.objects.utils.printException


class Task(private val runnable: Runnable) : Runnable {

    override fun run() {
        try {
            runnable.run()
        } catch (e: Exception) {
            e.printStackTrace()
            printException(Thread.currentThread(), e, null, null)
        }
    }
}