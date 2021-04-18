package me.melijn.melijnbot.database.games

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.commands.games.RockPaperScissorsGame
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.objectMapper

class RPSWrapper(private val rpsDao: RPSDao) {

    fun addGame(rpsGame: RockPaperScissorsGame) {
        val value = objectMapper.writeValueAsString(rpsGame)
        rpsDao.setCacheEntry(rpsGame.user1, value, NORMAL_CACHE)
        rpsDao.setCacheEntry(rpsGame.user2, value, NORMAL_CACHE)
        rpsDao.add(rpsGame)
    }

    fun removeGame(rpsGame: RockPaperScissorsGame) {
        rpsDao.removeCacheEntry(rpsGame.user1)
        rpsDao.removeCacheEntry(rpsGame.user2)
        rpsDao.remove(rpsGame.user1)
    }

    suspend fun getGame(userId: Long): RockPaperScissorsGame? {
        val cached = rpsDao.getCacheEntry(userId)?.run {
            if (isNotBlank()) objectMapper.readValue<RockPaperScissorsGame>(this)
            else null
        }
        if (cached != null) {
            return cached
        }
        val result = rpsDao.get(userId)
        rpsDao.setCacheEntry(userId, "", HIGHER_CACHE)
        return result
    }

    suspend fun getGames(): List<RockPaperScissorsGame> {
        return rpsDao.getAll()
    }

    fun removeGames(games: List<RockPaperScissorsGame>) {
        if (games.isEmpty()) return
        if (games.size == 1) removeGame(games.first())
        else {
            for (game in games) {
                rpsDao.removeCacheEntry(game.user1)
                rpsDao.removeCacheEntry(game.user2)
            }

            rpsDao.removeAll(games)
        }
    }
}