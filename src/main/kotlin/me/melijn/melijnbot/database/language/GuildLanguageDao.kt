package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class GuildLanguageDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "guildLanguages"
    override val tableStructure: String = "guildId bigint, language varchar(32)"
    override val keys: String = "PRIMARY KEY(guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
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

    suspend fun set(guildId: Long, language: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, language) VALUES (?, ?) ON CONFLICT (guildId) DO UPDATE SET language = ?",
            guildId, language, language)
    }

    suspend fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}