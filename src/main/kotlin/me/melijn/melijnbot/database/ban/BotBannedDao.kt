package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.database.locking.EntityType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BotBannedDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "bot_banned"
    override val tableStructure: String = "type smallint, id bigint, reason varchar(128), moment bigint"
    override val primaryKey: String = "id"

    override val cacheName: String = "botbanned"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(type: EntityType, id: Long, reason: String) {
        driverManager.executeUpdate(
            "INSERT INTO $table (type, id, reason, moment) VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING",
            type.id, id, reason, System.currentTimeMillis()
        )
    }

    fun remove(id: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE id = ?", id)
    }

    suspend fun contains(id: Long): Boolean = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ?", { rs ->
            it.resume(rs.next())
        }, id)
    }

    suspend fun getAll(type: EntityType): Set<BotBanInfo> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE type = ?", { rs ->
            val set = HashSet<BotBanInfo>()
            while (rs.next()) {
                set.add(
                    BotBanInfo(
                        rs.getLong("id"),
                        EntityType.values().first { it.id == rs.getByte("type") },
                        rs.getString("reason"),
                        rs.getLong("moment")
                    )
                )
            }
            it.resume(set)
        }, type.id)
    }

    suspend fun get(id: Long): BotBanInfo? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ?", { rs ->
            if (rs.next()) {
                it.resume(
                    BotBanInfo(
                        rs.getLong("id"),
                        EntityType.values().first { it.id == rs.getByte("type") },
                        rs.getString("reason"),
                        rs.getLong("moment")
                    )
                )
            } else {
                it.resume(null)
            }
        }, id)
    }
}