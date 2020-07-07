package me.melijn.melijnbot.objects.services.message

import me.melijn.melijnbot.database.message.MessageHistoryWrapper
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.RunnableTask
import java.util.concurrent.TimeUnit

class MessageCleanerService(private val messageHistoryWrapper: MessageHistoryWrapper) : Service("MessageCleaner", 12, 2, TimeUnit.HOURS) {

    override val service = RunnableTask {
        messageHistoryWrapper.clearOldMessages()
    }
}