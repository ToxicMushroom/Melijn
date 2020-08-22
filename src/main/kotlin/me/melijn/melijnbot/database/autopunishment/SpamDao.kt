package me.melijn.melijnbot.database.autopunishment

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.SpamType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SpamDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "spams"
    override val tableStructure: String = "guildId bigint, spamGroupName varchar(32), type varchar(32), optionMap varchar(2048)"
    override val primaryKey: String = "guildId, spamGroupName, type"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun add(guildId: Long, spamGroupName: String, type: SpamType, optionMap: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, spamGroupName, type, optionMap) VALUES (?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET optionMap = ?",
            guildId, spamGroupName, type.toString(), optionMap, optionMap)
    }

    suspend fun get(guildId: Long, spamGroupName: String, type: SpamType): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND spamGroupName = ? AND type = ?", { rs ->
            while (rs.next()) {
                it.resume(rs.getString("optionMap"))
            }
        }, guildId, spamGroupName, type.toString())
    }

    suspend fun remove(guildId: Long, spamGroupName: String, type: SpamType) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND spamGroupName = ? AND type = ?",
            guildId, spamGroupName, type.toString())
    }
}