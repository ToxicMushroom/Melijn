package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.objects.command.AbstractCommand

class CommandDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "commands"
    override val tableStructure: String = "id int, name varchar(64), description varchar(256), syntax varchar(256), help varchar(512), category varchar(64), aliases varchar(256), discordPermissions varchar(512), runConditions varchar(512)"
    override val primaryKey: String = "id"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun insert(command: AbstractCommand) {
        driverManager.executeUpdate("INSERT INTO $table (id, name, description, syntax, help, category, aliases, discordPermissions, runConditions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            command.id, command.name, command.description, command.syntax, command.help, command.commandCategory.toString(),
            command.aliases.joinToString("%S%"), command.discordPermissions.joinToString("%S%"), command.runConditions.joinToString("%S%"))
    }
}