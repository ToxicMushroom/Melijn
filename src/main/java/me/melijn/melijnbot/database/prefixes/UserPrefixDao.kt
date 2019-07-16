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

    fun getPrefixes(key: Long, prefixes: Consumer<String>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", Consumer { resultSet ->
            if (resultSet.next()) {
                prefixes.accept(resultSet.getString("prefixes"))
            }
        }, key)
    }
}