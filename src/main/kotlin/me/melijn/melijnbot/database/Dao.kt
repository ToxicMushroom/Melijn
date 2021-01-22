package me.melijn.melijnbot.database

import io.lettuce.core.SetArgs
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class Dao(val driverManager: DriverManager) {

    abstract val table: String
    abstract val tableStructure: String
    abstract val primaryKey: String

    open val uniqueKey: String = ""

    fun clear() {
        driverManager.clear(table)
    }

    suspend fun getRowCount() = suspendCoroutine<Long> {
        driverManager.executeQuery("SELECT COUNT(*) FROM $table", { rs ->
            rs.next()
            it.resume(rs.getLong(1))
        })
    }
}

abstract class CacheDao(val driverManager: DriverManager) {

    abstract val cacheName: String

    fun setCacheEntry(key: Any, value: Any, ttlM: Int? = null) =
        driverManager.setCacheEntry("$cacheName:$key", value.toString(), ttlM)

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) =
        driverManager.setCacheEntryWithArgs("$cacheName:$key", value.toString(), args)

    suspend fun getCacheEntry(key: Any, ttlM: Int? = null): String? =
        driverManager.getCacheEntry("$cacheName:$key", ttlM)

    fun removeCacheEntry(key: Any) =
        driverManager.removeCacheEntry("$cacheName:$key")
}

abstract class CacheDBDao(driverManager: DriverManager) : Dao(driverManager) {

    abstract val cacheName: String

    fun setCacheEntry(key: Any, value: Any, ttlM: Int? = null) =
        driverManager.setCacheEntry("$cacheName:$key", value.toString(), ttlM)

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) =
        driverManager.setCacheEntryWithArgs("$cacheName:$key", value.toString(), args)

    suspend fun getCacheEntry(key: Any, ttlM: Int? = null): String? =
        driverManager.getCacheEntry("$cacheName:$key", ttlM)

    fun removeCacheEntry(key: Any) =
        driverManager.removeCacheEntry("$cacheName:$key")

}