package me.melijn.melijnbot.database.reminder

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ReminderDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "reminders"
    override val tableStructure: String = "userId bigint, remindAt bigint, message varchar(2000)"
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

    suspend fun getPastReminders(moment: Long): List<Reminder> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE remindAt < ?", { rs ->
            val list = mutableListOf<Reminder>()

            while (rs.next()) {
                list.add(Reminder(
                    rs.getLong("userId"),
                    rs.getLong("remindAt"),
                    rs.getString("message")
                ))
            }

            it.resume(list)
        }, moment)
    }

    suspend fun getRemindersOfUser(userId: Long): List<Reminder> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            val list = mutableListOf<Reminder>()

            while (rs.next()) {
                list.add(Reminder(
                    rs.getLong("userId"),
                    rs.getLong("remindAt"),
                    rs.getString("message")
                ))
            }

            it.resume(list)
        }, userId)
    }

}

data class Reminder(
    val userId: Long,
    val remindAt: Long,
    val message: String
)