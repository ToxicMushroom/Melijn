package me.melijn.melijnbot.objects.services.birthday

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BirthdayService(val shardManager: ShardManager,
                      val daoManager: DaoManager)
    : Service("birthday") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    val birthdayService = Runnable {
        runBlocking {
            val birthdays = daoManager.birthdayWrapper.getBirthdaysToday()
            val rolls = shardManager.guilds
                .map { it.getAndVerifyRoleByType(daoManager, RoleType.BIRTHDAY, true) }
                .filter { it != null }
            rolls


        }

    }

    override fun start() {
        logger.info("Started BirthdayService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(birthdayService, 1_100, 1_000, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        logger.info("Stopping BirthdayService")
        scheduledFuture?.cancel(false)
    }
}