package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.ChannelRoleState
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class ChannelRoleDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "channel_roles"
    override val tableStructure: String = "guild_id bigint, channel_id bigint, role_id bigint, state varchar(32)"
    override val primaryKey: String = "guild_id, channel_id, role_id"

    override val cacheName: String = "channelRoles"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, channelId: Long, roleId: Long, state: ChannelRoleState) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guild_id, channel_id, role_id, state) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET state = ?",
            guildId, channelId, roleId, state.toString(), state.toString()
        )
    }

    fun remove(guildId: Long, channelId: Long, roleId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guild_id = ? AND channel_id = ? AND role_id = ?",
            guildId, channelId, roleId
        )
    }

    suspend fun getRoleIds(guildId: Long, channelId: Long): Map<ChannelRoleState, List<Long>> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guild_id = ? AND channel_id = ?", { rs ->
                val map = mutableMapOf<ChannelRoleState, List<Long>>()
                while (rs.next()) {
                    val state = ChannelRoleState.valueOf(rs.getString("state"))
                    val stateRoles = map[state]?.toMutableList() ?: mutableListOf()
                    stateRoles.addIfNotPresent(rs.getLong("role_id"))
                    map[state] = stateRoles
                }
                it.resume(map)
            }, guildId, channelId
        )
    }

    suspend fun getChannelRoles(guildId: Long): Map<Long, Map<ChannelRoleState, List<Long>>> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guild_id = ?", { rs ->
                val map: MutableMap<Long, MutableMap<ChannelRoleState, List<Long>>> = mutableMapOf()
                while (rs.next()) {
                    val channelId = rs.getLong("channel_id")
                    val channelMap = map[channelId] ?: mutableMapOf()
                    val state = ChannelRoleState.valueOf(rs.getString("state"))
                    val stateRoles = channelMap[state]?.toMutableList() ?: mutableListOf()
                    stateRoles.addIfNotPresent(rs.getLong("role_id"))
                    channelMap[state] = stateRoles
                    map[channelId] = channelMap
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