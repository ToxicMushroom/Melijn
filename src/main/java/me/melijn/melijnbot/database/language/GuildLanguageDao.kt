package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer


class GuildLanguageDao(private val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "guildLanguages"
    override val tableStructure: String = "guildId bigint, language varchar(32)"
    override val keys: String = "PRIMARY KEY(guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, language: Consumer<String>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", Consumer { resultset ->
            if (resultset.next()) {
                language.accept(resultset.getString("language"))
            } else language.accept("EN")

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