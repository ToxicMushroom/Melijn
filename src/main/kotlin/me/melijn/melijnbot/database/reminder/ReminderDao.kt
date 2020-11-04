package me.melijn.melijnbot.database.reminder

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReminderDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "reminders"
    override val tableStructure: String = "userId, remindAt, message"
    override val primaryKey: String = "userId, remindAt"
    
    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(userId: Long, remindAt: Long, message: String) {
        driverManager.executeUpdate("INSERT INTO $table (userId, remindAt, message) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
        userId, remindAt, message)
    }

    fun remove(userId: Long, remindAt: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ? AND remindAt = ?",
        userId, remindAt)
    }

    suspend fun getPastReminders(moment: Long): List<Reminder> = suspendCoroutine{
        driverManager.executeQuery("SELECT * FROM $table WHERE remindAt < moment",  { rs ->
            val list = mutableListOf<Reminder>()

            while (rs.next()) {
                list.add(Reminder(
                    rs.getLong("userId"),
                    rs.getLong("remindAt"),
                    rs.getLong("moment")
                ))
            }

            it.resume(list)
        }, moment)
    }
}
data class Reminder(
    val userId: Long,
    val remindAt: Long,
    val moment: Long
)