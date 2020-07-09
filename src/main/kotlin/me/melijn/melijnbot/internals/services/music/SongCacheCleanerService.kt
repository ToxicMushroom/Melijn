package me.melijn.melijnbot.internals.services.music

import me.melijn.melijnbot.database.audio.SongCacheWrapper
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import java.util.concurrent.TimeUnit

class SongCacheCleanerService(private val songCacheWrapper: SongCacheWrapper) : Service("SongCacheCleaner", 1, 1, TimeUnit.HOURS) {

    override val service = RunnableTask {
        songCacheWrapper.clearOldTracks()
    }
}