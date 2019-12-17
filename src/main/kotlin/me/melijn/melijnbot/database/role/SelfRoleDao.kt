package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "selfRoles"
    override val tableStructure: String = "guildId bigint, emoteji varchar(64), roleId bigint"
    override val primaryKey: String = "emoteji"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, emoteji: String, roleId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, emoteji, roleId) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET roleId = ?",
            guildId, emoteji, roleId, roleId)
    }

    suspend fun getMap(guildId: Long): Map<String, Long> = suspendCoroutine {
        val map = mutableMapOf<String, Long>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            while (rs.next()) {
                map[rs.getString("emoteji")] = rs.getLong("roleId")
            }
        }, guildId)
        it.resume(map)
    }

    suspend fun remove(guildId: Long, emoteji: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND emoteji = ?",
            guildId, emoteji)
    }


}