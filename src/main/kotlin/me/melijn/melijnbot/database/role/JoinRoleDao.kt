package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JoinRoleDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "joinRoles"
    override val tableStructure: String = "guildId bigint, joinRoleInfo varchar(2048)"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "roles:join"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("joinRoleInfo"))
            } else {
                it.resume("")
            }
        }, guildId)
    }

    fun put(guildId: Long, joinRoleInfo: String) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, joinRoleInfo) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET joinRoleInfo = ?",
            guildId, joinRoleInfo, joinRoleInfo
        )
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}