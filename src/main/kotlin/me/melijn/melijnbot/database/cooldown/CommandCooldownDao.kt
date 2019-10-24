package me.melijn.melijnbot.database.cooldown

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class CommandCooldownDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "commandCooldowns"
    override val tableStructure: String = "guildId bigint, commandId varchar(16), cooldown bigint"
    override val primaryKey: String = "guildId, commandId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getCooldowns(guildId: Long): Map<String, Long> = suspendCoroutine {
        val hashMap = HashMap<String, Long>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { resultSet ->
            while (resultSet.next()) {
                hashMap[resultSet.getString("commandId")] = resultSet.getLong("cooldown")
            }
            it.resume(hashMap.toMap())
        }, guildId)
    }

    suspend fun insert(guildId: Long, commandId: String, cooldown: Long) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, commandId, cooldown) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO NOTHING",
            guildId, commandId, cooldown)
    }

    fun bulkPut(guildId: Long, commandIds: Set<String>, cooldownMillis: Long) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("INSERT INTO $table (guildId, commandId, cooldownMillis) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET cooldownMillis = ?").use { preparedStatement ->
                preparedStatement.setLong(1, guildId)
                preparedStatement.setLong(3, cooldownMillis)
                preparedStatement.setLong(4, cooldownMillis)
                for (id in commandIds) {
                    preparedStatement.setString(2, id)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeBatch()
            }
        }
    }

    fun bulkDelete(guildId: Long, commandIds: Set<String>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement("DELETE FROM $table WHERE guildId = ? AND commandId = ?").use { preparedStatement ->
                preparedStatement.setLong(1, guildId)
                for (id in commandIds) {
                    preparedStatement.setString(2, id)
                    preparedStatement.addBatch()
                }
                preparedStatement.executeBatch()
            }
        }
    }
}