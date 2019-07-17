package me.melijn.melijnbot.database.language

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class UserLanguageDao(private val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "userLanguages"
    override val tableStructure: String = "userId bigint, language varchar(32)"
    override val keys: String = "PRIMARY KEY (userId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(userId: Long, language: Consumer<String>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", Consumer { resultset ->
            if (resultset.next()) {
                language.accept(resultset.getString("language"))
            } else language.accept("EN")

        }, userId)
    }

    fun set(userId: Long, language: String) {
        driverManager.executeUpdate("INSERT INTO $table (userId, language) VALUES (?, ?) ON DUPLICATE KEY UPDATE language = ?",
                userId, language, language)
    }
}