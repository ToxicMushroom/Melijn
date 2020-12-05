package me.melijn.melijnbot.database.embed

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class EmbedDisabledDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "embedDisabled"
    override val tableStructure: String = "guildId bigint"
    override val primaryKey: String = "guildId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
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
        driverManager.executeUpdate(
            "INSERT INTO $table (guildId) VALUES (?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId
        )
    }

    fun remove(guildId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ?", guildId)
    }
}