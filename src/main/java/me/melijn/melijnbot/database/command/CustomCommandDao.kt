package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.database.message.ModularMessage

class CustomCommandDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "customCommands"
    override val tableStructure: String =
        "guildId bigint, id bigint auto_increment, name varchar(64)," +
            " description varchar(256), content varchar(4096), aliases varchar(128)," +
            " chance int, prefix boolean, enabled boolean"
    override val keys: String = "PRIMARY KEY (id)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun add(guildId: Long, cc: CustomCommand): Long {
        cc.apply {
            return driverManager.executeUpdateGetGeneratedKeys("INSERT INTO $table (guildId, name, description, content, aliases, chance, prefix, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                guildId, name, description, content.toJSON(), aliases?.joinToString("%SPLIT%"), chance, prefix, enabled)
        }
    }

    suspend fun set(guildId: Long, id: Int, cc: CustomCommand) {
        cc.apply {
            driverManager.executeUpdate("UPDATE $table SET name = ?, description = ?, content = ?, aliases = ?, chance = ?, prefix = ?, enabled = ? WHERE guildId = ? AND id = ?",
                name, description, content, aliases?.joinToString("%SPLIT%"), chance, prefix, enabled, guildId, id)
        }
    }

    suspend fun remove(guildId: Long, id: Int) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND id = ?",
            guildId, id)
    }

    suspend fun getForGuild(guildId: Long): Map<Long, CustomCommand> {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", guildId).use { rs ->
            val map = HashMap<Long, CustomCommand>()

            while (rs.next()) {
                val cc = CustomCommand(
                    rs.getString("name"),
                    ModularMessage.fromJSON(rs.getString("content")),
                    rs.getNString("description"),
                    rs.getNString("aliases")?.split("%SPLIT%")?.toList(),
                    rs.getInt("chance"),
                    rs.getBoolean("prefix"),
                    rs.getBoolean("enabled")
                )
                map[rs.getLong("id")] = cc
            }
            return map
        }
    }
}

data class CustomCommand(
    val name: String,
    val content: ModularMessage,
    var description: String? = null,
    val aliases: List<String>? = null,
    val chance: Int = 100,
    val prefix: Boolean = true,
    val enabled: Boolean = true
)