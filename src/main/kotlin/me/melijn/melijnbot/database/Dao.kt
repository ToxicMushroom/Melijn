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

    fun setCacheEntry(key: String, value: String, args: SetArgs? = null) {
        val async = driverManager.redisConnection.async()
        (if (args == null) async.set(key, value, args)
        else async.set(key, value)).handleAsync { t, u ->
            println("$t - $u")
        }
    }

    suspend fun getCacheEntry(key: String): String? {
        return driverManager.redisConnection.async()
            .get(key).await()
    }
}