package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class UserEmbedColorDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "userEmbedColors"
    override val tableStructure: String = "userId bigint, color int"
    override val keys: String = "PRIMARY KEY (userId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(userId: Long, color: Consumer<Int>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", Consumer {
            if (it.next()) color.accept(it.getInt("color"))
            else color.accept(-1)
        }, userId)
    }

    fun set(userId: Long, color: Int) {
        driverManager.executeUpdate("INSERT INTO $table (userId, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color = ?",
                userId, color, color)
    }
}