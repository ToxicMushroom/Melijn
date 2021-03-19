package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.utils.splitIETEL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CustomCommandDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "custom_commands"
    override val tableStructure: String =
        "guild_id bigint, id BIGSERIAL, name varchar(64)," +
            " description varchar(256), msg_name varchar(64), aliases varchar(128)," +
            " chance int, prefix boolean, contains_triggers boolean"
    override val primaryKey: String = "id"

    override val cacheName: String = "command:custom"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun add(guildId: Long, cc: CustomCommand): Long {
        cc.apply {
            return driverManager.executeUpdateGetGeneratedKeys(
                "INSERT INTO $table (guild_id, name, description, msg_name, aliases, chance, prefix, contains_triggers) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                guildId, name, description, msgName, aliases?.joinToString("%SPLIT%"), chance, prefix, containsTriggers
            )
        }
    }

    fun update(guildId: Long, cc: CustomCommand) {
        cc.apply {
            driverManager.executeUpdate(
                "UPDATE $table SET name = ?, description = ?, msg_name = ?, aliases = ?, chance = ?, prefix = ?, contains_triggers = ? WHERE guild_id = ? AND id = ?",
                name, description, msgName, aliases?.joinToString("%SPLIT%"), chance, prefix, containsTriggers, guildId, id
            )
        }
    }

    fun remove(guildId: Long, id: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guild_id = ? AND id = ?", guildId, id)
    }

    suspend fun getForGuild(guildId: Long): List<CustomCommand> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guild_id = ?", { rs ->
            val list = ArrayList<CustomCommand>()

            while (rs.next()) {
                val cc = CustomCommand(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("msg_name"),
                    rs.getString("description"),
                    rs.getString("aliases")?.splitIETEL("%SPLIT%") ?: emptyList(),
                    rs.getInt("chance"),
                    rs.getBoolean("prefix"),
                    rs.getBoolean("contains_triggers")
                )
                list.add(cc)
            }
            it.resume(list)
        }, guildId)
    }
}

data class CustomCommand(
    var id: Long,
    var name: String,
    var msgName: String,
    var description: String? = null,
    var aliases: List<String>? = null,
    var chance: Int = 100,
    var prefix: Boolean = true,
    var containsTriggers: Boolean = true
)