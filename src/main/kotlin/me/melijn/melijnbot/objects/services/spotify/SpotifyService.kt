package me.melijn.melijnbot.objects.services.spotify

import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import me.melijn.melijnbot.objects.web.WebManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SpotifyService(val webManager: WebManager) : Service("spotify") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val spotifyService = Task {
        webManager.updateSpotifyCredentials()
    }

    override fun start() {
        logger.info("Started SpotifyService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(spotifyService, 1_800_000, 1_800_000, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        logger.info("Stopping SpotifyService")
        scheduledFuture?.cancel(false)
    }
}