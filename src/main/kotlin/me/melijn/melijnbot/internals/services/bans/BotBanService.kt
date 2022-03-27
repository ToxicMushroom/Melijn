package me.melijn.melijnbot.internals.services.bans

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.locking.EntityType
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class BotBanService(
    val shardManager: ShardManager, val daoManager: DaoManager
) : Service("BotBan", 20, 0, TimeUnit.SECONDS) {

    override val service: RunnableTask = RunnableTask {
        val news = daoManager.botBannedWrapper.renew()
        for (new in news) {
            if (new.entityType == EntityType.GUILD) {
                shardManager.getGuildById(new.id)?.leave()?.queue()
            }
        }
    }
}