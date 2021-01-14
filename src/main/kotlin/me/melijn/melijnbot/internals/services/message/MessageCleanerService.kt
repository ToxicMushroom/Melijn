package me.melijn.melijnbot.internals.services.message

import me.melijn.melijnbot.database.message.MessageHistoryWrapper
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import java.util.concurrent.TimeUnit

class MessageCleanerService(private val messageHistoryWrapper: MessageHistoryWrapper) :
    Service("MessageCleaner", 12, 2, TimeUnit.HOURS) {

    override val service = RunnableTask {
        messageHistoryWrapper.clearOldMessages()
    }
}