package me.melijn.melijnbot.objects.services.bans

import me.melijn.melijnbot.objects.services.Service
import java.util.concurrent.TimeUnit

class BanService() : Service("ban") {

    val banService = {

    }

    init {
        scheduledExecutor.scheduleWithFixedDelay(banService, 1_000, 1_000, TimeUnit.MILLISECONDS)
    }
}