package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.util.function.Consumer

class EmbedDisabledDao(val driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "embedDisabled"
    override val tableStructure: String = "guildId bigint"
    override val keys: String = "PRIMARY KEY (guildId)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getSet(set: (Set<Long>) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table", {
            val hashSet = HashSet<Long>()
            while (it.next()) {
                hashSet.add(it.getLong("guildId"))
            }
            set.invoke(hashSet)
        })
    }

    fun add(guildId: Long) {
        driverManager.executeUpdate("INSERT IGNORE INTO $table (guildId) VALUES (?)", guildId)
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}