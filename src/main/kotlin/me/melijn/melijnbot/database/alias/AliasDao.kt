package me.melijn.melijnbot.database.alias

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.utils.splitIETEL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// free: 5 rows, 1 value each | premium: 50 rows, 5 value each
class AliasDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "aliases"
    override val tableStructure: String = "id bigint, command varchar(128), aliases varchar(256)"
    override val primaryKey: String = "id, command"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun insert(id: Long, commandNode: String, aliases: String) {
        driverManager.executeUpdate("INSERT INTO $table (id, command, aliases) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET aliases = ?",
            id, commandNode, aliases, aliases)
    }

    fun remove(id: Long, commandNode: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE id = ? AND command = ?", id, commandNode)
    }

    suspend fun getAliases(id: Long): Map<String, List<String>> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ?", { rs ->
            val map = mutableMapOf<String, List<String>>()
            while (rs.next()) {
                val aliases = rs.getString("aliases").splitIETEL("%SPLIT%")
                map[rs.getString("command")] = aliases
            }
            it.resume(map)
        }, id)
    }

    suspend fun clear(id: Long, command: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE id = ? AND command = ?",
            id, command)
    }
}