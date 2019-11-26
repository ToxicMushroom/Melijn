package me.melijn.melijnbot.objects.services.spotify

import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.web.WebManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SpotifyService(val webManager: WebManager) : Service("spotify") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val spotifyService = Runnable {
        webManager.updateSpotifyCredentials()
    }

    override fun start() {
        logger.info("Started MuteService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(spotifyService, 1_100, 1_000, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        logger.info("Stopping SpotifyService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(spotifyService, 1_100, 1_000, TimeUnit.MILLISECONDS)
    }
}