package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AutoPunishmentDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "autoPunishments"
    override val tableStructure: String = "guildId bigint, userId bigint, pointsMap varchar(1024), expire bigint"
    override val primaryKey: String = "guildId, userId, expire"

    override val cacheName: String = "autopunishment"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long, userId: Long): Map<ExpireTime, String> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND userId = ?", { rs ->
            val map = mutableMapOf<ExpireTime, String>()

            while (rs.next()) {
                map[rs.getLong("expire")] = rs.getString("pointsMap")
            }

            it.resume(map)
        }, guildId, userId)
    }

    fun set(guildId: Long, userId: Long, pointsMap: String, expire: Long) {
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId, userId, pointsMap, expire) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET pointsMap = ?",
            guildId, userId, pointsMap, expire, pointsMap
        )
    }

    fun bulkSet(guildId: Long, userId: Long, sqlMap: MutableMap<ExpireTime, String>) {
        driverManager.getUsableConnection {
            val statement =
                it.prepareStatement("INSERT INTO $table (guildId, userId, pointsMap, expire) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET pointsMap = ?")
            statement.setLong(1, guildId)
            statement.setLong(2, userId)
            for ((expire, pointsMap) in sqlMap) {
                statement.setString(3, pointsMap)
                statement.setLong(4, expire)
                statement.setString(5, pointsMap)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    fun removeEntriesOlderThen(time: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE expire < ?", time)
    }
}