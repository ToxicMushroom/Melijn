package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class ChannelRoleDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "channel_roles"
    override val tableStructure: String = "guild_id bigint, channel_id bigint, role_id bigint"
    override val primaryKey: String = "guild_id, channel_id, role_id"

    override val cacheName: String = "channelRoles"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(guildId: Long, channelId: Long, roleId: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guild_id, channel_id, role_id) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, channelId, roleId
        )
    }

    fun remove(guildId: Long, channelId: Long, roleId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guild_id = ? AND channel_id = ? AND role_id = ?",
            guildId, channelId, roleId
        )
    }

    suspend fun getRoleIds(guildId: Long, channelId: Long): List<Long> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guild_id = ? AND channel_id = ?", { rs ->
                val list = mutableListOf<Long>()
                while (rs.next()) {
                    list.add(
                        rs.getLong("role_id")
                    )
                }
                it.resume(list)
            }, guildId, channelId
        )
    }

    suspend fun getChannelRoles(guildId: Long): Map<Long, List<Long>> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guild_id = ?", { rs ->
                val map = mutableMapOf<Long, List<Long>>()
                while (rs.next()) {
                    val channelId = rs.getLong("channel_id")
                    val roleId = rs.getLong("role_id")
                    val cRoles = map[channelId]?.toMutableList() ?: mutableListOf()
                    cRoles.addIfNotPresent(roleId)
                    map[channelId] = cRoles
                }
                it.resume(map)
            }, guildId
        )
    }

    fun clear(guildId: Long, channelId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guild_id = ? AND channel_id = ?",
            guildId, channelId
        )
    }
}