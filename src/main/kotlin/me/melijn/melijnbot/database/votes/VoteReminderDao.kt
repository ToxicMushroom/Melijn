package me.melijn.melijnbot.database.votes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VoteReminderDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "voteReminders"
    override val tableStructure: String = "userId bigint, flag int, remindAt bigint"
    override val primaryKey: String = "userId, flag"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun addReminder(userId: Long, flag: Int, remindAt: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (userId, flag, remindAt) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET remindAt = ?",
            userId, flag, remindAt, remindAt
        )
    }

    suspend fun getReminders(beforeMillis: Long): List<VoteReminder> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE remindAt < ?", { rs ->
            val list = mutableListOf<VoteReminder>()

            while (rs.next()) {
                list.add(
                    VoteReminder(
                        rs.getLong("userId"),
                        rs.getInt("flag"),
                        rs.getLong("remindAt")
                    )
                )
            }

            it.resume(list)
        }, beforeMillis)
    }

    fun removeReminder(userId: Long, flag: Int) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE userId = ? AND flag = ?",
            userId, flag
        )
    }

    fun removeReminders(beforeMillis: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE remindAt < ?", beforeMillis)
    }

    fun removeReminders(userIds: Map<Long, List<Int>>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE userId = ? AND flag = ?").use { statement ->
                for ((userId, flags) in userIds) {
                    statement.setLong(1, userId)
                    for (flag in flags) {
                        statement.setInt(2, flag)
                        statement.addBatch()
                    }
                }
                statement.executeBatch()
            }
        }
    }

    suspend fun get(userId: Long): List<VoteReminder> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            val list = mutableListOf<VoteReminder>()
            while (rs.next()) {
                list.add(
                    VoteReminder(rs.getLong("userId"), rs.getInt("flag"), rs.getLong("remindAt"))
                )
            }
            it.resume(list)
        }, userId)
    }

}

data class VoteReminder(
    val userId: Long,
    val flag: Int,
    val remindAt: Long
)