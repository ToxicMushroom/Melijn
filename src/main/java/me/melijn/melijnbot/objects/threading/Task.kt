package me.melijn.melijnbot.objects.threading

import me.melijn.melijnbot.objects.utils.MessageUtils

class Task(private val messageUtils: MessageUtils, private val runnable: Runnable) : Runnable {

    override fun run() {
        try {
            runnable.run()
        } catch (e: Exception) {
            e.printStackTrace()
            messageUtils.printException(Thread.currentThread(), e, null, null)
        }
    }
}