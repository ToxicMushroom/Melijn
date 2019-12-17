package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class UserEmbedColorDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "userEmbedColors"
    override val tableStructure: String = "userId bigint, color int"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun get(userId: Long, color: (Int) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", {
            if (it.next()) color.invoke(it.getInt("color"))
            else color.invoke(0)
        }, userId)
    }

    suspend fun set(userId: Long, color: Int) {
        driverManager.executeUpdate("INSERT INTO $table (userId, color) VALUES (?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET color = ?",
            userId, color, color)
    }

    suspend fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?",
            userId)
    }
}