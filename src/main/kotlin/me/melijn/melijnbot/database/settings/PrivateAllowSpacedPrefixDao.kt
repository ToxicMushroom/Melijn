package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.models.TriState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PrivateAllowSpacedPrefixDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "privateAllowSpacedPrefixStates"
    override val tableStructure: String = "userId bigint, state boolean"
    override val primaryKey: String = "userId"

    override val cacheName: String = "private:allowspacedprefix"

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

    fun setState(userId: Long, state: Boolean) {
        driverManager.executeUpdate("INSERT INTO $table (userId, state) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET state = ?",
            userId, state, state)
    }

    fun delete(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }
}