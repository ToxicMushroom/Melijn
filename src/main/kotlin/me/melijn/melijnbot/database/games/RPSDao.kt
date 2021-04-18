package me.melijn.melijnbot.database.games

import me.melijn.melijnbot.commands.games.RockPaperScissorsGame
import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import java.sql.ResultSet
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RPSDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "active_rps_games"
    override val tableStructure: String =
        "user1 bigint, user2 bigint, bet bigint, choice1 varchar(128), choice2 varchar(128), startTime bigint"
    override val primaryKey: String = "user1"
    override val uniqueKey: String = "user2"

    override val cacheName: String = "ACTIVE:RPS_GAMES"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }

    fun add(game: RockPaperScissorsGame) {
        game.run {
            val c1 = choice1.toString()
            val c2 = choice2.toString()
            driverManager.executeUpdate(
                "INSERT INTO $table (user1, user2, bet, choice1, choice2, startTime) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET choice1 = ?, choice2 = ?",
                user1, user2, bet, c1, c2, startTime, c1, c2
            )
        }
    }

    fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE user1 = ? OR user2 = ?", userId, userId)
    }

    suspend fun get(userId: Long): RockPaperScissorsGame? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE user1 = ? OR user2 = ?", { rs ->
            if (rs.next()) {
                it.resume(
                    RockPaperScissorsGame(
                        rs.getLong("user1"),
                        rs.getLong("user2"),
                        rs.getLong("bet"),
                        parseChoice(rs, "choice1"),
                        parseChoice(rs, "choice2"),
                        rs.getLong("startTime")
                    )
                )
            } else it.resume(null)
        }, userId, userId)
    }

    private fun parseChoice(rs: ResultSet, column: String) = try {
        RockPaperScissorsGame.RPS.valueOf(rs.getString(column))
    } catch (t: Throwable) {
        null
    }

    suspend fun getAll(): List<RockPaperScissorsGame> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table", { rs ->
            val list = mutableListOf<RockPaperScissorsGame>()
            while (rs.next()) {
                list.add(
                    RockPaperScissorsGame(
                        rs.getLong("user1"),
                        rs.getLong("user2"),
                        rs.getLong("bet"),
                        parseChoice(rs, "choice1"),
                        parseChoice(rs, "choice2"),
                        rs.getLong("startTime")
                    )
                )
            }
            it.resume(list)
        })
    }

    fun removeAll(games: List<RockPaperScissorsGame>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE user1 = ? OR user2 = ?").use { prep ->
                for (game in games) {
                    prep.setLong(1, game.user1)
                    prep.setLong(2, game.user2)
                    prep.addBatch()
                }
                prep.executeBatch()
            }
        }
    }
}