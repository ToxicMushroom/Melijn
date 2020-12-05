package me.melijn.melijnbot.database.giveaway

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GiveawayDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "giveaways"
    override val tableStructure: String =
        "guildId bigint, channelId bigint, messageId bigint, winners int, prize varchar(128), endTime bigint"
    override val primaryKey: String = "guildId, messageId"

    override val cacheName: String = "giveaway"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun insertOrUpdate(guildId: Long, giveaway: Giveaway) {
        giveaway.apply {
            driverManager.executeUpdate(
                "INSERT INTO $table (guildId, channelId, messageId, winners, prize, endTime) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET winners = ?, prize = ?, endTime = ?",
                guildId, channelId, messageId, winners, prize, endTime, winners, prize, endTime
            )
        }
    }

    suspend fun getGiveaways(guildId: Long): List<Giveaway> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val giveaways = mutableListOf<Giveaway>()

            if (rs.next()) {
                giveaways.add(
                    Giveaway(
                        rs.getLong("channelId"),
                        rs.getLong("messageId"),
                        rs.getInt("winners"),
                        rs.getString("prize"),
                        rs.getLong("endTime")
                    )
                )
            }

            it.resume(giveaways)
        }, guildId)
    }

    fun remove(guildId: Long, messageId: Long) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guildId = ? AND messageId = ?",
            guildId, messageId
        )
    }
}

data class Giveaway(
    val channelId: Long,
    val messageId: Long,
    val winners: Int,
    val prize: String,
    val endTime: Long
)