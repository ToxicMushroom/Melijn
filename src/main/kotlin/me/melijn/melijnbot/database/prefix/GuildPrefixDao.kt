package me.melijn.melijnbot.database.prefix

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GuildPrefixDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "guildPrefixes"
    override val tableStructure: String = "guildId bigInt, prefixes text"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "prefixes:guild"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, prefixes: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, prefixes) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET prefixes = ?",
            guildId, prefixes, prefixes)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { resultSet ->
            if (resultSet.next()) {
                it.resume(resultSet.getString("prefixes"))
            } else {
                it.resume("")
            }
        }, guildId)
    }
}