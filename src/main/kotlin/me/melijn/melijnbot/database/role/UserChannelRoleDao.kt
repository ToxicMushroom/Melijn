package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.ChannelRoleState
import me.melijn.melijnbot.internals.utils.addIfNotPresent
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserChannelRoleDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val cacheName: String = "userChannelRoles"
    override val table: String = "user_channel_roles"
    override val tableStructure: String = "user_id bigint, role_id bigint, state varchar(32)"

    override val primaryKey: String = "user_id, role_id"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun setBulk(userId: Long, map: Map<ChannelRoleState, List<Long>>) {
        driverManager.getUsableConnection {
            val statement = it.prepareStatement(
                "INSERT INTO $table (user_id, role_id, state) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET state = ?"
            )
            statement.setLong(1, userId)
            for ((state, roles) in map) {
                statement.setString(3, state.toString())
                statement.setString(4, state.toString())
                for (role in roles) {
                    statement.setLong(2, role)
                    statement.addBatch()
                }
            }
            statement.executeBatch()
        }
    }

    suspend fun get(userId: Long): Map<ChannelRoleState, List<Long>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE user_id = ?", { rs ->
            val map = mutableMapOf<ChannelRoleState, List<Long>>()
            while (rs.next()) {
                val state = ChannelRoleState.valueOf(rs.getString("state"))
                val list = map[state]?.toMutableList() ?: mutableListOf()
                list.addIfNotPresent(rs.getLong("role_id"))
                map[state] = list
            }
            it.resume(map)
        }, userId)
    }

    fun clear(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE user_id = ?", userId)
    }
}