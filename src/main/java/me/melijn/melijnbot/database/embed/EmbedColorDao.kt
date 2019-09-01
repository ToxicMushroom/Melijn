package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class EmbedColorDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "embedColors"
    override val tableStructure: String = "guildId bigint, color int"
    override val keys: String = "PRIMARY KEY (guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, color: (Int) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", {
            if (it.next()) color.invoke(it.getInt("color"))
            else color.invoke(-1)
        }, guildId)
    }

    fun set(guildId: Long, color: Int) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color = ?",
                guildId, color, color)
    }
}