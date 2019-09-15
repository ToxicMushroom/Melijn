package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class UserLanguageDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "userLanguages"
    override val tableStructure: String = "userId bigint, language varchar(32)"
    override val keys: String = "PRIMARY KEY (userId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(userId: Long, language: (String) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { resultset ->
            if (resultset.next()) {
                language.invoke(resultset.getString("language"))
            } else language.invoke("")

        }, userId)
    }

    suspend fun set(userId: Long, language: String) {
        driverManager.executeUpdate("INSERT INTO $table (userId, language) VALUES (?, ?) ON DUPLICATE KEY UPDATE language = ?",
            userId, language, language)
    }

    suspend fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }
}