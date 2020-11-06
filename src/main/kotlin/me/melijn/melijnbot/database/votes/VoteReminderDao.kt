package me.melijn.melijnbot.database.votes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VoteReminderDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "voteReminders"
    override val tableStructure: String = "userId bigint, remindAt bigint"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun addReminder(userId: Long, remindAt: Long) {
        driverManager.executeUpdate("INSERT INTO $table (userId, remindAt) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET remindAt = ?",
            userId, remindAt, remindAt)
    }

    suspend fun getReminders(beforeMillis: Long): List<VoteReminder> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE remindAt < ?", { rs ->
            val list = mutableListOf<VoteReminder>()

            while (rs.next()) {
                list.add(VoteReminder(
                    rs.getLong("userId"),
                    rs.getLong("remindAt")
                ))
            }

            it.resume(list)
        }, beforeMillis)
    }

    fun removeReminder(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }

    fun removeReminders(beforeMillis: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE remindAt < ?", beforeMillis)
    }

    fun removeReminders(userIds: MutableList<Long>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE userId = ?").use { statement ->
                for (userId in userIds) {
                    statement.setLong(1, userId)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    suspend fun get(userId: Long): Long? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("remindAt"))
            } else {
                it.resume(null)
            }
        }, userId)
    }


}

data class VoteReminder(
    val userId: Long,
    val remindAt: Long
)