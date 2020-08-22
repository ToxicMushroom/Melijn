package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.RoleType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RoleDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "roles"
    override val tableStructure: String = "guildId bigInt, roleType varchar(32), roleId bigint"
    override val primaryKey: String = "guildId, roleType"

    override val cacheName: String = "role"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, roleType: RoleType, roleId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, roleType, roleId) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET roleId = ?",
            guildId, roleType.toString(), roleId, roleId)
    }

    fun unset(guildId: Long, roleType: RoleType) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND roleType = ?", guildId, roleType.toString())
    }

    suspend fun get(guildId: Long, roleType: RoleType): Long = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND roleType = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getLong("roleId"))
            } else {
                it.resume(-1)
            }
        }, guildId, roleType.toString())
    }

    suspend fun getRoles(roleType: RoleType): Map<Long, Long> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE roleType = ?", { rs ->
            val roleMap = HashMap<Long, Long>()
            while (rs.next()) {
                roleMap[rs.getLong("guildId")] = rs.getLong("roleId")
            }
            it.resume(roleMap)
        }, roleType.toString())
    }
}