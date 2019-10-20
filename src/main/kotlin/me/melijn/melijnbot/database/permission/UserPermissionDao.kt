package me.melijn.melijnbot.database.permission

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.PermState
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserPermissionDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "userPermissions"
    override val tableStructure: String = "guildId bigint, userId bigint UNIQUE, permission varchar(64) UNIQUE, state varchar(8)"
    override val keys: String = ""

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    fun getPermState(userId: Long, permission: String, permState: (PermState) -> Unit) {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ? AND permission = ?", { resultset ->
            if (resultset.next()) {
                permState.invoke(PermState.valueOf(resultset.getString("state")))
            } else {
                permState.invoke(PermState.DEFAULT)
            }
        }, userId, permission)
    }

    suspend fun set(guildId: Long, userId: Long, permission: String, permState: PermState) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, userId, permission, state) VALUES (?, ?, ?, ?) ON CONFLICT (userId, permission) DO UPDATE state = ?",
            guildId, userId, permission, permState.toString(), permState.toString())
    }

    suspend fun delete(guildId: Long, userId: Long, permission: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND userId = ? AND permission = ?", guildId, userId, permission)
    }

    suspend fun delete(guildId: Long, userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND userId = ?", guildId, userId)
    }

    suspend fun getMap(guildId: Long, userId: Long): Map<String, PermState> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ? AND guildId = ?", { resultset ->
            val map = HashMap<String, PermState>()
            while (resultset.next()) {
                map[resultset.getString("permission")] = PermState.valueOf(resultset.getString("state"))
            }
            it.resume(map)
        }, userId, guildId)
    }

    fun bulkPut(guildId: Long, userId: Long, permissions: List<String>, state: PermState) {
        driverManager.getUsableConnection { connection ->
            connection.prepareStatement("INSERT INTO $table (guildId, userId, permission, state) VALUES (?, ?, ?, ?) ON CONFLICT (userId, permission) DO UPDATE state = ?").use { statement ->
                statement.setLong(1, guildId)
                statement.setLong(2, userId)
                statement.setString(4, state.toString())
                statement.setString(5, state.toString())
                for (perm in permissions) {
                    statement.setString(3, perm)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }

    fun bulkDelete(guildId: Long, userId: Long, permissions: List<String>) {
        driverManager.getUsableConnection { connection ->
            connection.prepareStatement("DELETE FROM $table WHERE guildId =? AND userId = ? AND permission = ?").use { statement ->
                statement.setLong(1, guildId)
                statement.setLong(2, userId)
                for (perm in permissions) {
                    statement.setString(3, perm)
                    statement.addBatch()
                }
                statement.executeLargeBatch()
            }
        }
    }
}
