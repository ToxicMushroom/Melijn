package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.internals.TriState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PrivateAllowSpacedPrefixDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "privateAllowSpacedPrefixStates"
    override val tableStructure: String = "userId bigint, state boolean"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getState(userId: Long): TriState = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            if (rs.next()) {
                val state = when (rs.getBoolean("state")) {
                    true -> TriState.TRUE
                    false -> TriState.FALSE
                }
                it.resume(state)
            } else {
                it.resume(TriState.DEFAULT)
            }
        }, userId)
    }

    suspend fun setState(userId: Long, state: Boolean) {
        driverManager.executeUpdate("INSERT INTO $table (userId, state) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET state = ?",
            userId, state, state)
    }

    suspend fun delete(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }
}