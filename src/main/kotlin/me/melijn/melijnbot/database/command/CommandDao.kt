package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.utils.splitIETEL
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CommandDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "commands"
    override val tableStructure: String =
        "id int, name varchar(64), description varchar(256), syntax varchar(256), help varchar(512), category varchar(64), aliases varchar(256), discordPermissions varchar(512), runConditions varchar(512)"
    override val primaryKey: String = "id"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

//    suspend fun insert(command: AbstractCommand) {
//        driverManager.executeUpdate("INSERT INTO $table (id, name, description, syntax, help, category, aliases, discordPermissions, runConditions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
//            command.id, command.name, command.description, command.syntax, command.help, command.commandCategory.toString(),
//            command.aliases.joinToString("%S%"), command.discordChannelPermissions.joinToString("%S%"), command.runConditions.joinToString("%S%"))
//    }

    suspend fun bulkInsert(commands: HashSet<AbstractCommand>) {
        val sql =
            "INSERT INTO $table (id, name, description, syntax, help, category, aliases, discordPermissions, runConditions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET " +
                "name = ?, description = ?, syntax = ?, help = ?, category = ?, aliases = ?, discordPermissions = ?, runConditions = ?"

        val commandsInDatabase = mutableListOf<DBCommand>()
        driverManager.executeQuery("SELECT * FROM $table", { rs ->
            while (rs.next()) {
                commandsInDatabase.add(
                    DBCommand(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("syntax"),
                        rs.getString("help"),
                        rs.getString("category"),
                        rs.getString("aliases"),
                        rs.getString("discordPermissions"),
                        rs.getString("runConditions")
                    )
                )
            }
        })

        val nextId = IntObj(getNextId())
        driverManager.getUsableConnection {
            it.prepareStatement(sql).use { preparedStatement ->
                for (command in commands) {
                    val newId = if (command.id == 0) {
                        getId(commandsInDatabase, command.name, command.aliases, nextId)
                    } else command.id

                    command.id = newId
                    preparedStatement.setObject(1, newId)
                    preparedStatement.setObject(2, command.name)
                    preparedStatement.setObject(3, command.description)
                    preparedStatement.setObject(4, command.syntax)
                    preparedStatement.setObject(5, command.help)
                    preparedStatement.setObject(6, command.commandCategory.toString())
                    preparedStatement.setObject(7, command.aliases.joinToString("%S%"))
                    preparedStatement.setObject(8, command.discordChannelPermissions.joinToString("%S%"))
                    preparedStatement.setObject(9, command.runConditions.joinToString("%S%"))
                    preparedStatement.setObject(10, command.name)
                    preparedStatement.setObject(11, command.description)
                    preparedStatement.setObject(12, command.syntax)
                    preparedStatement.setObject(13, command.help)
                    preparedStatement.setObject(14, command.commandCategory.toString())
                    preparedStatement.setObject(15, command.aliases.joinToString("%S%"))
                    preparedStatement.setObject(16, command.discordChannelPermissions.joinToString("%S%"))
                    preparedStatement.setObject(17, command.runConditions.joinToString("%S%"))
                    preparedStatement.addBatch()
                }
                preparedStatement.executeBatch()
            }
        }
    }

    private suspend fun getNextId(): Int = suspendCoroutine {
        driverManager.executeQuery("SELECT id FROM $table ORDER BY id DESC LIMIT 1", { rs ->
            if (rs.next()) {
                it.resume(rs.getInt("id"))
            } else {
                it.resume(0)
            }
        })
    }

    fun getId(commandsInDatabase: List<DBCommand>, name: String, aliases: Array<String>, newId: IntObj): Int {
        val match = commandsInDatabase.firstOrNull { dbCmd ->
            dbCmd.name.equals(name, true) ||
                dbCmd.aliases.splitIETEL("%S%").any {
                name.equals(it, true)
            } || aliases.any {
                dbCmd.name.equals(it, true)
            }
        } ?: return newId.incAndGet()

        return match.id
    }

    data class DBCommand(
        val id: Int,
        val name: String,
        val description: String,
        val syntax: String,
        val help: String,
        val category: String,
        val aliases: String,
        val discordChannelPerms: String,
        val runConditions: String
    )
}

class IntObj(var i: Int) {

    fun incAndGet(): Int {
        return ++i
    }

    fun inc() {
        i++
    }

    fun dec() {
        i--
    }

    fun set(newI: Int) {
        i = newI
    }

    fun get(): Int {
        return i
    }
}