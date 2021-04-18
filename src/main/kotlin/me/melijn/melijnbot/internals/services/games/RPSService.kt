package me.melijn.melijnbot.internals.services.games

import me.melijn.melijnbot.commands.games.RockPaperScissorsGame
import me.melijn.melijnbot.commandutil.game.RockPaperScissors
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.sharding.ShardManager

class RSPService(shardManager: ShardManager, daoManager: DaoManager) : Service("rps", 5, 20) {

    override val service: RunnableTask = RunnableTask {
        val toRemove = mutableListOf<RockPaperScissorsGame>()
        val games = daoManager.rpsWrapper.getGames()
        val l = System.currentTimeMillis() - 300_000
        shardManager.shards.firstOrNull()?.let { jda ->
            games.forEach { game ->
                if (RockPaperScissors.checkForContinue(jda, daoManager, game)) {
                    toRemove.add(game)
                } else if (game.startTime < l) {
                    daoManager.balanceWrapper.addBalance(game.user1, game.bet)
                    daoManager.balanceWrapper.addBalance(game.user2, game.bet)
                    val s = "RPS Game expired, refunded your **${game.bet}** mel"
                    jda.retrieveUserById(game.user1).awaitOrNull()?.openPrivateChannel()?.awaitOrNull()?.sendMessage(s)
                        ?.queue()
                    jda.retrieveUserById(game.user2).awaitOrNull()?.openPrivateChannel()?.awaitOrNull()?.sendMessage(s)
                        ?.queue()

                    toRemove.add(game)
                }
            }
        }
        daoManager.rpsWrapper.removeGames(toRemove)
    }
}