package me.melijn.melijnbot.database.prefixes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class GuildPrefixDao(private val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "guildPrefixes"
    override val tableStructure: String = "guildId bigInt, prefixes varchar(256)"
    override val keys: String = "PRIMARY KEY(guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun setPrefixes(key: Long, prefixes: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildIdd, prefixes) VALUES (?, ?) ON DUPLICATE KEY UPDATE prefixes = ?",
                key, prefixes, prefixes)
    }

    fun getPrefixes(key: Long, prefixes: Consumer<String>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", Consumer { resultSet ->
            if (resultSet.next()) {
                prefixes.accept(resultSet.getString("prefixes"))
            } else prefixes.accept("")
        }, key)
    }
}