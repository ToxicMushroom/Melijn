package me.melijn.melijnbot.database.scripts

import com.fasterxml.jackson.module.kotlin.readValue
import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objectMapper
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ScriptDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val cacheName: String = "scripts"

    override val table: String = "scripts"
    override val tableStructure: String =
        "id bigint, use_prefix boolean, trigger varchar(128), commands varchar(2048), enabled boolean"
    override val primaryKey: String = "id, trigger"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun addScript(entityId: Long, usePrefix: Boolean, trigger: String, commands: String, enabled: Boolean) {
        driverManager.executeUpdate(
            "INSERT INTO $table (id, use_prefix, trigger, commands, enabled) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET " +
                "use_prefix = ?, trigger = ?, commands = ?, enabled = ?",
            entityId,
            usePrefix,
            trigger,
            commands,
            enabled,
            usePrefix,
            trigger,
            commands,
            enabled
        )
    }

    fun removeScript(entityId: Long, trigger: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE id = ? AND trigger = ?", entityId, trigger)
    }

    suspend fun getScript(entityId: Long, trigger: String): Script? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ? AND trigger = ?", { rs ->
            if (rs.next()) {
                it.resume(
                    Script(
                        rs.getBoolean("use_prefix"),
                        rs.getString("trigger"),
                        objectMapper.readValue(rs.getString("commands")),
                        rs.getBoolean("enabled")
                    )
                )
            }
        }, entityId, trigger)
    }

    suspend fun getScripts(entityId: Long): List<Script> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ?", { rs ->
            val list = mutableListOf<Script>()
            while (rs.next()) {
                list.add(
                    Script(
                        rs.getBoolean("use_prefix"),
                        rs.getString("trigger"),
                        objectMapper.readValue(rs.getString("commands")),
                        rs.getBoolean("enabled")
                    )
                )
            }
            it.resume(list)
        }, entityId)
    }


}

typealias ScriptBody = Map<Int, Pair<String, List<String>>>

data class Script(
    val usePrefix: Boolean,
    val trigger: String,
    val commands: ScriptBody,
    val enabled: Boolean
)