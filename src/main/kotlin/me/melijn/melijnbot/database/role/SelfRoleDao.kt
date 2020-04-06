package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "selfRoles"
    override val tableStructure: String = "guildId bigint, groupName varchar(64), emoteji varchar(64), roleId bigint"
    override val primaryKey: String = "guildId, groupName, emoteji"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, groupName: String, emoteji: String, roleId: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, groupName, emoteji, roleId) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET roleId = ?",
            guildId, groupName, emoteji, roleId, roleId)
    }

    suspend fun getMap(guildId: Long): Map<String, Map<String, List<Long>>> = suspendCoroutine {
        val map = mutableMapOf<String, Map<String, List<Long>>>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val group = rs.getString("groupName")
            val pairs = map.getOrDefault(group, emptyMap()).toMutableMap()
            while (rs.next()) {
                val emoteji = rs.getString("emoteji")
                pairs[emoteji] = pairs.getOrDefault(emoteji, emptyList()) + rs.getLong("roleId")
            }
            map[group] = pairs
        }, guildId)
        it.resume(map)
    }

    suspend fun remove(guildId: Long, groupName: String, emoteji: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND groupName = ? AND emoteji = ?",
            guildId, groupName, emoteji)
    }
}