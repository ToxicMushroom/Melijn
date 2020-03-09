package me.melijn.melijnbot.objects.services.music

import me.melijn.melijnbot.database.audio.SongCacheWrapper
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class SongCacheCleanerService(private val songCacheWrapper: SongCacheWrapper) : Service("songcachecleaner") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val songCacheCleanerService = Task {
        songCacheWrapper.clearOldTracks()
    }

    override fun start() {
        logger.info("Started SongCacheCleanerService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(songCacheCleanerService, 1, 1, TimeUnit.HOURS)
    }

    override fun stop() {
        logger.info("Stopping SongCacheCleanerService")
        scheduledFuture?.cancel(false)
    }
}