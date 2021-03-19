package me.melijn.melijnbot.internals.services.rockpaperscissors

import me.melijn.melijnbot.commands.games.RockPaperScissorsCommand
import me.melijn.melijnbot.commandutil.game.RockPaperScissors
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import net.dv8tion.jda.api.sharding.ShardManager

class RSPService(shardManager: ShardManager, daoManager: DaoManager) : Service("rps", 1, 20) {

    override val service: RunnableTask = RunnableTask {
        shardManager.shards.firstOrNull()?.let {
            val iter = RockPaperScissorsCommand.activeGames.listIterator()
            while (iter.hasNext()) {
                RockPaperScissors.checkForContinue(it, daoManager, iter)
            }
        }
    }
}