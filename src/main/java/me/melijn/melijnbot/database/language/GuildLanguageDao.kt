package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager


class GuildLanguageDao(private val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "guildLanguages"
    override val tableStructure: String = "guildId bigint, language varchar(32)"
    override val keys: String = "PRIMARY KEY(guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, language: (String) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { resultset ->
            if (resultset.next()) {
                language.invoke(resultset.getString("language"))
            } else language.invoke("EN")

        }, guildId)
    }

    fun set(guildId: Long, language: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, language) VALUES (?, ?) ON DUPLICATE KEY UPDATE language = ?",
                guildId, language, language)
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}