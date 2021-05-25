package me.melijn.melijnbot.database.message

class MessageHistoryWrapper(private val messageHistoryDao: MessageHistoryDao) {

    private val attachmentUrl = "https://cdn.discordapp.com/attachments/"

    suspend fun getMessageById(messageId: Long): DaoMessage? {
        val msg = messageHistoryDao.get(messageId)
        if (msg != null) msg.attachments = increaseAttachments(msg)
        return msg
    }

    fun addMessage(daoMessage: DaoMessage) {
        reduceAttachments(daoMessage)
        messageHistoryDao.add(daoMessage)
    }

    fun setMessage(daoMessage: DaoMessage) {
        reduceAttachments(daoMessage)
        messageHistoryDao.set(daoMessage)
    }

    fun clearOldMessages() {
        messageHistoryDao.clearOldMessages()
    }

    suspend fun getMessagesByIds(map: List<Long>): List<DaoMessage> {
        val many = messageHistoryDao.getMany(map)
        many.map { it.attachments = increaseAttachments(it) }
        return many
    }

    private fun reduceAttachments(daoMessage: DaoMessage) {
        daoMessage.attachments = daoMessage.attachments.map { it.removePrefix(attachmentUrl) }
    }

    private fun increaseAttachments(it: DaoMessage) = it.attachments.map { attach ->
        if (!attach.startsWith("http")) "$attachmentUrl$attach"
        else attach
    }

    fun addMessages(results: List<DaoMessage>) {
        messageHistoryDao.add(results)
    }
}