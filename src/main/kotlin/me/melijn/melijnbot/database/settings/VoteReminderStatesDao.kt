package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VoteReminderStatesDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "voteRemindersStates"
    override val tableStructure: String = "userId bigint, option int"
    override val primaryKey: String = "userId, option"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(userId: Long, option: Int) {
        driverManager.executeUpdate("INSERT INTO $table (userId, option) VALUES (?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            userId, option)
    }

    fun delete(userId: Long, option: Int) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ? AND option = ?",
            userId, option)
    }

    suspend fun getFlags(userId: Long): List<Int> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            val states = mutableListOf<Int>()
            while (rs.next()) {
                states.add(rs.getInt("option"))
            }

            it.resume(states)
        }, userId)
    }
}
