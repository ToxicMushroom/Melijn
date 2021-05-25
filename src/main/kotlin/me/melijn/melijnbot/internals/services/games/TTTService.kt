package me.melijn.melijnbot.internals.services.games

import me.melijn.melijnbot.commands.games.TicTacToeGame
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.sharding.ShardManager

class TTTService(shardManager: ShardManager, daoManager: DaoManager) : Service("ttt", 5, 20) {

    override val service: RunnableTask = RunnableTask {
        val toRemove = mutableListOf<TicTacToeGame>()
        val tttWrapper = daoManager.tttWrapper
        val games = tttWrapper.getGames()
        val l = System.currentTimeMillis() - 300_000
        shardManager.shards.firstOrNull()?.let { jda ->
            games.forEach { game ->
                if (game.lastUpdate < l) {
                    daoManager.balanceWrapper.addBalance(game.user1, game.bet)
                    daoManager.balanceWrapper.addBalance(game.user2, game.bet)
                    val s = "TTT Game expired, refunded your **${game.bet}** mel"
                    jda.retrieveUserById(game.user1).awaitOrNull()?.openPrivateChannel()?.awaitOrNull()?.sendMessage(s)
                        ?.queue()
                    jda.retrieveUserById(game.user2).awaitOrNull()?.openPrivateChannel()?.awaitOrNull()?.sendMessage(s)
                        ?.queue()

                    toRemove.add(game)
                }
            }
        }
        tttWrapper.removeGames(toRemove)
    }
}