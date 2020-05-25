package me.melijn.melijnbot.database.alias

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// free: 5 rows, 1 value each | premium: 50 rows, 5 value each
class GuildAliasDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "guildAliases"
    override val tableStructure: String = "guildId bigint, command varchar(64), aliases varchar(128)"
    override val primaryKey: String = "guildId, command"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun insert(guildId: Long, commandNode: String, aliases: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, command, aliases) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET aliases = ?",
            guildId, commandNode, aliases)
    }

    suspend fun remove(guildId: Long, commandNode: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND command = ?", guildId, commandNode)
    }

    suspend fun getAliases(guildId: Long, commandNode: String): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND command = ?", { rs ->
            while (rs.next()) {
                if (rs.next()) {
                    it.resume(rs.getString("aliases"))
                } else {
                    it.resume("")
                }
            }
        }, guildId, commandNode)
    }


}