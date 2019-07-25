package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class EmbedColorDao(val driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "embedColors"
    override val tableStructure: String = "guildId bigint, color int"
    override val keys: String = "PRIMARY KEY (guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun get(guildId: Long, color: Consumer<Int>) {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", Consumer {
            if (it.next()) color.accept(it.getInt("color"))
            else color.accept(-1)
        }, guildId)
    }

    fun set(guildId: Long, color: Int) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, color) VALUES (?, ?) ON DUPLICATE KEY UPDATE color = ?",
                guildId, color, color)
    }
}