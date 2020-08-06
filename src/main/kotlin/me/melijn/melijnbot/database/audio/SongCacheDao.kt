package me.melijn.melijnbot.database.audio

import me.melijn.melijnbot.database.CacheDao
import me.melijn.melijnbot.database.DriverManager

class SongCacheDao(driverManager: DriverManager) : CacheDao(driverManager) {

    override val cacheName: String = "songcache"

}