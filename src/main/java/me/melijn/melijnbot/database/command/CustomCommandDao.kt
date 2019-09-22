package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.database.message.ModularMessage

class CustomCommandDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "customCommands"
    override val tableStructure: String =
        "guildId bigint, id bigint auto_increment, name varchar(64)," +
            " description varchar(256), content varchar(4096), aliases varchar(128)," +
            " chance int, prefix boolean"
    override val keys: String = "PRIMARY KEY (id)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
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
                name, description, content, aliases?.joinToString("%SPLIT%"), chance, prefix, guildId, cc.id)
        }
    }

    suspend fun remove(guildId: Long, id: Long) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND id = ?",
            guildId, id)
    }

    suspend fun getForGuild(guildId: Long): List<CustomCommand> {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", guildId).use { rs ->
            val list = ArrayList<CustomCommand>()

            while (rs.next()) {
                val cc = CustomCommand(
                    rs.getLong("id"),
                    rs.getString("name"),
                    ModularMessage.fromJSON(rs.getString("content")),
                    rs.getNString("description"),
                    rs.getNString("aliases")?.split("%SPLIT%")?.toList(),
                    rs.getInt("chance"),
                    rs.getBoolean("prefix")
                )
                list.add(cc)
            }
            return list
        }
    }
}

data class CustomCommand(
    var id: Long,
    val name: String,
    var content: ModularMessage,
    var description: String? = null,
    var aliases: List<String>? = null,
    var chance: Int = 100,
    var prefix: Boolean = true
)