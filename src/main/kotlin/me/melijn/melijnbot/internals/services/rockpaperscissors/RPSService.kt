package me.melijn.melijnbot.internals.services.rockpaperscissors

import me.melijn.melijnbot.commands.games.RockPaperScissorsCommand
import me.melijn.melijnbot.commands.games.RockPaperScissorsGame
import me.melijn.melijnbot.commandutil.game.RockPaperScissors
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import net.dv8tion.jda.api.sharding.ShardManager

class RSPService(shardManager: ShardManager, daoManager: DaoManager) : Service("rps", 1, 20) {

    override val service: RunnableTask = RunnableTask {
        val toRemove = mutableListOf<RockPaperScissorsGame>()
        shardManager.shards.firstOrNull()?.let { jda ->
            RockPaperScissorsCommand.activeGames.forEach {
                if (RockPaperScissors.checkForContinue(jda, daoManager, it)) {
                    toRemove.add(it)
                }
            }
        }
        RockPaperScissorsCommand.activeGames.removeAll(toRemove)
    }
}