package me.melijn.melijnbot.database.permission

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class DiscordChannelOverridesDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "discord_channel_overrides"
    override val tableStructure: String =
        "guild_id bigint, channel_id bigint, id bigint, allowed_flag bigint, denied_flag bigint" // id is either a role or a user
    override val primaryKey: String = "guild_id, channel_id, id"

    override val cacheName: String = "discordchanneloverrides"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun put(guildId: Long, channelId: Long, id: Long, denied: Long, allowed: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guild_id, channel_id, id, allowed_flag, denied_flag) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey)" +
                " DO UPDATE SET allowed_flag = ?, denied_flag = ?",
            guildId, channelId, id, allowed, denied, allowed, denied
        )
    }

    fun bulkPut(guildId: Long, channelId: Long, map: Map<Long, Pair<Long, Long>>) {
        driverManager.getUsableConnection {
            val statement = it.prepareStatement(
                "INSERT INTO $table (guild_id, channel_id, id, allowed_flag, denied_flag) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey)" +
                    " DO UPDATE SET allowed_flag = ?, denied_flag = ?"
            )
            statement.setLong(1, guildId)
            statement.setLong(2, channelId)

            for ((id, flags) in map) {
                statement.setLong(3, id)
                statement.setLong(4, flags.first)
                statement.setLong(5, flags.second)
                statement.setLong(6, flags.first)
                statement.setLong(7, flags.second)
                statement.addBatch()
            }

            statement.executeBatch()
        }
    }


    fun remove(guildId: Long, channelId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guild_id = ? AND channel_id = ?",
            guildId, channelId
        )
    }

    suspend fun getAll(guildId: Long, channelId: Long): Map<Long, Pair<Long, Long>> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE guild_id = ? AND channel_id = ?",
            { rs ->
                val map = mutableMapOf<Long, Pair<Long, Long>>() // id -> <allowed, denied>

                while (rs.next()) {
                    map[rs.getLong("id")] = rs.getLong("allowed_flag") to rs.getLong("denied_flag")
                }

                it.resume(map)
            }, guildId, channelId
        )
    }

    fun removeAll(guildId: Long, list: List<Long>) {
        driverManager.getUsableConnection {
            val statement = it.prepareStatement(
                "DELETE FROM $table WHERE guild_id = ? AND channel_id = ?"
            )
            statement.setLong(1, guildId)

            for (id in list) {
                statement.setLong(2, id)
                statement.addBatch()
            }

            statement.executeBatch()
        }
    }
}