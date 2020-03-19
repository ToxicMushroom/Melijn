package me.melijn.melijnbot.objects.services.music

import me.melijn.melijnbot.database.audio.SongCacheWrapper
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import java.util.concurrent.TimeUnit

class SongCacheCleanerService(private val songCacheWrapper: SongCacheWrapper) : Service("SongCacheCleaner",1, 1, TimeUnit.HOURS) {

    override val service = Task {
        songCacheWrapper.clearOldTracks()
    }
}