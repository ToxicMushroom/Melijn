package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GuildLanguageDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "guildLanguages"
    override val tableStructure: String = "guildId bigint, language varchar(32)"
    override val primaryKey: String = "guildId"

    override val cacheName: String = "language:guild"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun get(guildId: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { resultset ->
            if (resultset.next()) {
                it.resume(resultset.getString("language"))
            } else {
                it.resume("EN")
            }

        }, guildId)
    }

    fun set(guildId: Long, language: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, language) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET language = ?",
            guildId, language, language)
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}