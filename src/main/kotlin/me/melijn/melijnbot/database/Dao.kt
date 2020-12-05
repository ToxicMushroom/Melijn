package me.melijn.melijnbot.database

import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await
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

    fun setCacheEntry(key: Any, value: Any, ttlM: Int? = null) {
        val async = driverManager.redisConnection?.async() ?: return
        if (ttlM == null) async.set("$cacheName:$key", value.toString())
        else async.set("$cacheName:$key", value.toString(), SetArgs().ex(ttlM * 60L))
    }

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) {
        val async = driverManager.redisConnection?.async() ?: return
        if (args == null) async.set("$cacheName:$key", value.toString())
        else async.set("$cacheName:$key", value.toString(), args)
    }

    // ttl: minutes
    suspend fun getCacheEntry(key: Any, newTTL: Int? = null): String? {
        val commands = (driverManager.redisConnection?.async() ?: return null)
        val result = commands
            .get("$cacheName:$key")
            .await()
        if (result != null && newTTL != null) {
            commands.expire("$cacheName:$key", newTTL * 60L)
        }
        return result
    }
}

abstract class CacheDBDao(driverManager: DriverManager) : Dao(driverManager) {

    abstract val cacheName: String

    // ttl: minutes
    fun setCacheEntry(key: Any, value: Any, ttlM: Int? = null) {
        val async = (driverManager.redisConnection?.async() ?: return)
        if (ttlM == null) async.set("$cacheName:$key", value.toString())
        else async.set("$cacheName:$key", value.toString(), SetArgs().ex(ttlM * 60L))
    }

    fun setCacheEntryWithArgs(key: Any, value: Any, args: SetArgs? = null) {
        val async = (driverManager.redisConnection?.async() ?: return)
        if (args == null) async.set("$cacheName:$key", value.toString())
        else async.set("$cacheName:$key", value.toString(), args)
    }

    // ttl: minutes
    suspend fun getCacheEntry(key: Any, newTTL: Int? = null): String? {
        val commands = (driverManager.redisConnection?.async() ?: return null)
        val result = commands
            .get("$cacheName:$key")
            .await()
        if (result != null && newTTL != null) {
            commands.expire("$cacheName:$key", newTTL * 60L)
        }
        return result
    }

    fun removeCacheEntry(key: Any) {
        driverManager.redisConnection?.async()
            ?.del(key.toString())
    }

    fun removeCacheEntryByPrefix(key: Any) {
        driverManager.redisConnection?.async()
            ?.del(key.toString())
    }
}