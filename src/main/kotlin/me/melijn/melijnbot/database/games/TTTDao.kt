package me.melijn.melijnbot.database.games

import me.melijn.melijnbot.commands.games.TicTacToeGame
import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.utils.splitIETEL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TTTDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "active_ttt_games"
    override val tableStructure: String =
        "user1 bigint, user2 bigint, bet bigint, game varchar(1024), lastUpdate bigint, startTime bigint"
    override val primaryKey: String = "user1"
    override val uniqueKey: String = "user2"

    override val cacheName: String = "ACTIVE:TTT_GAMES"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }

    fun add(game: TicTacToeGame) {
        val sql =
            "INSERT INTO $table (user1, user2, bet, game, startTime, lastUpdate) VALUES (?, ?, ?, ?, ?, ?)" +
                " ON CONFLICT ($primaryKey) DO UPDATE SET game = ?, lastUpdate = ?"
        game.run {
            val gameBody = this.gameState.joinToString(",") { it.toString() }
            driverManager.executeUpdate(
                sql,
                user1, user2, bet, gameBody, lastUpdate, startTime, gameBody, lastUpdate
            )
        }
    }

    fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE user1 = ? OR user2 = ?", userId, userId)
    }

    suspend fun get(userId: Long): TicTacToeGame? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE user1 = ? OR user2 = ?", { rs ->
            if (rs.next()) {
                it.resume(
                    TicTacToeGame(
                        rs.getLong("user1"),
                        rs.getLong("user2"),
                        rs.getLong("bet"),
                        rs.getString("game").splitIETEL(",").map { TicTacToeGame.TTTState.valueOf(it) }.toTypedArray(),
                        rs.getLong("lastUpdate"),
                        rs.getLong("startTime")
                    )
                )
            } else it.resume(null)
        }, userId, userId)
    }

    suspend fun getAll(): List<TicTacToeGame> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table", { rs ->
            val list = mutableListOf<TicTacToeGame>()
            while (rs.next()) {
                list.add(
                    TicTacToeGame(
                        rs.getLong("user1"),
                        rs.getLong("user2"),
                        rs.getLong("bet"),
                        rs.getString("game").splitIETEL(",").map { TicTacToeGame.TTTState.valueOf(it) }.toTypedArray(),
                        rs.getLong("lastUpdate"),
                        rs.getLong("startTime")
                    )
                )
            }
            it.resume(list)
        })
    }

    fun removeAll(games: List<TicTacToeGame>) {
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