package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageIdsDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "messageIds"
    override val tableStructure: String = "guildId bigint, idType varchar(32), ids varchar(2048)"
    override val primaryKey: String = "guildId, idType"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, idType: String, ids: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, idType, ids) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET ids = ?",
            guildId, idType, ids, ids)
    }

    suspend fun get(guildId: Long, idType: String): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND idType = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("ids"))
            } else {
                it.resume("")
            }
        }, guildId, idType)
    }
}