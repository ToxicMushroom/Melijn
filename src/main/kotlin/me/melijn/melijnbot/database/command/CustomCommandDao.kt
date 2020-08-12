package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.database.message.ModularMessage
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CustomCommandDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "customCommands"
    override val tableStructure: String =
        "guildId bigint, id BIGSERIAL, name varchar(64)," +
            " description varchar(256), content varchar(4096), aliases varchar(128)," +
            " chance int, prefix boolean"
    override val primaryKey: String = "id"

    override val cacheName: String = "command:custom"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, cc: CustomCommand): Long {
        cc.apply {
            return driverManager.executeUpdateGetGeneratedKeys("INSERT INTO $table (guildId, name, description, content, aliases, chance, prefix) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guildId, name, description, content.toJSON(), aliases?.joinToString("%SPLIT%"), chance, prefix)
        }
    }

    suspend fun update(guildId: Long, cc: CustomCommand) {
        cc.apply {
            driverManager.executeUpdate("UPDATE $table SET name = ?, description = ?, content = ?, aliases = ?, chance = ?, prefix = ? WHERE guildId = ? AND id = ?",
                name, description, content.toJSON(), aliases?.joinToString("%SPLIT%"), chance, prefix, guildId, cc.id)
        }
    }

    suspend fun remove(guildId: Long, id: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND id = ?",
            guildId, id)
    }

    suspend fun getForGuild(guildId: Long): List<CustomCommand> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val list = ArrayList<CustomCommand>()

            while (rs.next()) {
                val cc = CustomCommand(
                    rs.getLong("id"),
                    rs.getString("name"),
                    ModularMessage.fromJSON(rs.getString("content")),
                    rs.getString("description"),
                    rs.getString("aliases")?.split("%SPLIT%")?.toList(),
                    rs.getInt("chance"),
                    rs.getBoolean("prefix")
                )
                val aliases = cc.aliases
                if (aliases?.size == 1 && aliases[0].isEmpty()) {
                    cc.aliases = emptyList()
                }
                list.add(cc)
            }
            it.resume(list)
        }, guildId)
    }
}

data class CustomCommand(
    var id: Long,
    var name: String,
    var content: ModularMessage,
    var description: String? = null,
    var aliases: List<String>? = null,
    var chance: Int = 100,
    var prefix: Boolean = true
)