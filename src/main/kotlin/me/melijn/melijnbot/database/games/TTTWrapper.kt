package me.melijn.melijnbot.database.games

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.commands.games.TicTacToeGame
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class TTTWrapper(private val tttDao: TTTDao) {

    fun addGame(tttGame: TicTacToeGame) {
        val value = objectMapper.writeValueAsString(tttGame)
        tttDao.setCacheEntry(tttGame.user1, value, NORMAL_CACHE)
        tttDao.setCacheEntry(tttGame.user2, value, NORMAL_CACHE)
        tttDao.add(tttGame)
    }

    fun removeGame(tttGame: TicTacToeGame) {
        tttDao.removeCacheEntry(tttGame.user1)
        tttDao.removeCacheEntry(tttGame.user2)
        tttDao.remove(tttGame.user1)
    }

    suspend fun getGame(userId: Long): TicTacToeGame? {
        val cached = tttDao.getCacheEntry(userId)?.run {
            if (isNotBlank()) objectMapper.readValue<TicTacToeGame>(this)
            else null
        }
        if (cached != null) {
            return cached
        }
        val result = tttDao.get(userId)
        tttDao.setCacheEntry(userId, "", HIGHER_CACHE)
        return result
    }

    suspend fun getGames(): List<TicTacToeGame> {
        return tttDao.getAll()
    }

    fun removeGames(games: List<TicTacToeGame>) {
        if (games.isEmpty()) return
        if (games.size == 1) removeGame(games.first())
        else {
            for (game in games) {
                tttDao.removeCacheEntry(game.user1)
                tttDao.removeCacheEntry(game.user2)
            }

            tttDao.removeAll(games)
        }
    }

    fun clear() {
        tttDao.clear()
    }
}