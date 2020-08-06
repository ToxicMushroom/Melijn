package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleModeDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "selfRoleModes"
    override val tableStructure: String = "guildId bigint, mode varchar(16)"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "selfrole:mode"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun setMode(guildId: Long, mode: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, mode) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET mode = ?",
            guildId, mode, mode)
    }

    fun delete(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }

    suspend fun getMode(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("mode"))
            } else {
                SelfRoleMode.AUTO
            }
        }, guildId)
    }
}

enum class SelfRoleMode {
    AUTO, MANUAL
}