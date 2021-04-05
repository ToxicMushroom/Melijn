package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    val logger: Logger = LoggerFactory.getLogger(TestCommand::class.java)

    fun execute(context: ICommandContext) {

        context.daoManager.driverManager.executeQuery("SELECT * FROM userEmbedColors", { rs ->
            val userColorMap = mutableMapOf<Long, Int>()
            while (rs.next()) {
                userColorMap[rs.getLong("userId")] = rs.getInt("color")
            }

            println("migrating ${userColorMap.size} columns")

            var count = 1
            for ((userId, color) in userColorMap) {
                context.daoManager.embedColorWrapper.setColor(userId, color)
                println("migrated ${++count}/${userColorMap.size} columns")
            }
        })
    }
}
