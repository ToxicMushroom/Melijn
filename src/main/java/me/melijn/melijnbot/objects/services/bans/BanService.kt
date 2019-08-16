package me.melijn.melijnbot.objects.services.bans

import me.melijn.melijnbot.database.ban.BanWrapper
import me.melijn.melijnbot.objects.services.Service
import java.util.concurrent.TimeUnit

class BanService(banWrapper: BanWrapper) : Service("ban") {

    private val banService = Runnable {
        val bans = banWrapper.getUnbannableBans()
        bans.forEach { println(it) }
    }

    fun start() {
        scheduledExecutor.scheduleWithFixedDelay(banService, 1_000, 1_000, TimeUnit.MILLISECONDS)
    }
}