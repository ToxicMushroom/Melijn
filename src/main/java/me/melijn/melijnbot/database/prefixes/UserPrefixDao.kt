package me.melijn.melijnbot.database.prefixes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class UserPrefixDao(private val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "userPrefixes"
    override val tableStructure: String = "userId bigint, prefixes varchar(256)"
    override val keys: String = "PRIMARY KEY (userId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun set(userId: Long, prefixes: String) {
        driverManager.executeUpdate("INSERT INTO $table (userId, prefixes) VALUES (?, ?) ON DUPLICATE KEY UPDATE prefixes = ?",
                userId, prefixes, prefixes)
    }

    fun get(userId: Long, prefixes: (String) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { resultSet ->
            if (resultSet.next()) {
                prefixes.invoke(resultSet.getString("prefixes"))
            } else prefixes.invoke("")
        }, userId)
    }
}