package me.melijn.melijnbot.database.message

class MessageHistoryWrapper(private val messageHistoryDao: MessageHistoryDao) {

    suspend fun getMessageById(messageId: Long): DaoMessage? {
        return messageHistoryDao.get(messageId)
    }

    fun addMessage(daoMessage: DaoMessage) {
        messageHistoryDao.add(daoMessage)
    }

    fun setMessage(daoMessage: DaoMessage) {
        messageHistoryDao.set(daoMessage)
    }

    fun clearOldMessages() {
        messageHistoryDao.clearOldMessages()
    }

    suspend fun getMessagesByIds(map: List<Long>): List<DaoMessage> {
        return messageHistoryDao.getMany(map)
    }

    fun addMessages(results: List<DaoMessage>) {
        messageHistoryDao.add(results)
    }
}