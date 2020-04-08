package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleModeDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "selfRoleModes"
    override val tableStructure: String = "guildId bigint, mode varchar(16)"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun setMode(guildId: Long, mode: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, mode) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET mode = ?",
            guildId, mode, mode)
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

    suspend fun delete(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}

enum class SelfRoleMode {
    AUTO, MANUAL
}