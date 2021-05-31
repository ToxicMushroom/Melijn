package me.melijn.melijnbot.database

import com.fasterxml.jackson.module.kotlin.readValue
import io.lettuce.core.SetArgs
import me.melijn.melijnbot.objectMapper
import java.util.concurrent.TimeUnit
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

interface CacheUtil {

    val cacheName: String
    val driverManager: DriverManager

    fun setCacheEntry(key: Any, value: Any, ttl: Int? = null, timeUnit: TimeUnit = TimeUnit.MINUTES) {
        if (value is String || value is Int || value is Long || value is Double || value is Byte || value is Short ||
            value is Short || value is Enum<*>
        ) {
            driverManager.setCacheEntry("$cacheName:$key", value.toString(), ttl, timeUnit)
        } else {
            driverManager.setCacheEntry("$cacheName:$key", objectMapper.writeValueAsString(value), ttl, timeUnit)
        }
    }

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) =
        driverManager.setCacheEntryWithArgs("$cacheName:$key", value.toString(), args)

    suspend fun getCacheEntry(key: Any, ttlM: Int? = null): String? =
        driverManager.getCacheEntry("$cacheName:$key", ttlM)

    suspend fun getIntFromCache(key: Any, ttlM: Int? = null): Int? = getCacheEntry(key, ttlM)?.toIntOrNull()
    suspend fun getLongFromCache(key: Any, ttlM: Int? = null): Long? = getCacheEntry(key, ttlM)?.toLongOrNull()
    suspend fun getDoubleFromCache(key: Any, ttlM: Int? = null): Double? = getCacheEntry(key, ttlM)?.toDoubleOrNull()
    suspend fun getFloatFromCache(key: Any, ttlM: Int? = null): Float? = getCacheEntry(key, ttlM)?.toFloatOrNull()

    fun removeCacheEntry(key: Any) =
        driverManager.removeCacheEntry("$cacheName:$key")
}

suspend inline fun <reified K> CacheUtil.getValueFromCache(key: Any, ttlM: Int? = null): K? {
    return getCacheEntry(key, ttlM)?.let { objectMapper.readValue<K>(it) }
}

abstract class CacheDao(override val driverManager: DriverManager) : CacheUtil
abstract class CacheDBDao(driverManager: DriverManager) : Dao(driverManager), CacheUtil

