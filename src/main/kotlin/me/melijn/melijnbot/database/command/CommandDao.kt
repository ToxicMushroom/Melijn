package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.command.AbstractCommand
import java.util.*

class CommandDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "commands"
    override val tableStructure: String = "id int, name varchar(64), description varchar(256), syntax varchar(256), help varchar(512), category varchar(64), aliases varchar(256), discordPermissions varchar(512), runConditions varchar(512)"
    override val primaryKey: String = "id"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

//    suspend fun insert(command: AbstractCommand) {
//        driverManager.executeUpdate("INSERT INTO $table (id, name, description, syntax, help, category, aliases, discordPermissions, runConditions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
//            command.id, command.name, command.description, command.syntax, command.help, command.commandCategory.toString(),
//            command.aliases.joinToString("%S%"), command.discordChannelPermissions.joinToString("%S%"), command.runConditions.joinToString("%S%"))
//    }

    fun bulkInsert(commands: HashSet<AbstractCommand>) {
        val sql = "INSERT INTO $table (id, name, description, syntax, help, category, aliases, discordPermissions, runConditions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
        driverManager.getUsableConnection {
            it.prepareStatement(sql).use { preparedStatement ->
                for (command in commands) {
                    preparedStatement.setObject(1, command.id)
                    preparedStatement.setObject(2, command.name)
                    preparedStatement.setObject(3, command.description)
                    preparedStatement.setObject(4, command.syntax)
                    preparedStatement.setObject(5, command.help)
                    preparedStatement.setObject(6, command.commandCategory.toString())
                    preparedStatement.setObject(7, command.aliases.joinToString("%S%"))
                    preparedStatement.setObject(8, command.discordChannelPermissions.joinToString("%S%"))
                    preparedStatement.setObject(9, command.runConditions.joinToString("%S%"))
                    preparedStatement.addBatch()
                }
                preparedStatement.executeBatch()
            }
        }
    }
}