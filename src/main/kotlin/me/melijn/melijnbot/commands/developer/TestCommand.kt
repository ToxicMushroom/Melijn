package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.database.message.DaoMessage
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

    override suspend fun execute(context: ICommandContext) {
        context.daoManager.driverManager.executeQuery("SELECT * FROM historymessages", { rs ->
            val results = mutableListOf<DaoMessage>()
            var count = 0
            while (rs.next()) {
                val result = DaoMessage(
                    rs.getLong("guildId"),
                    rs.getLong("textChannelId"),
                    rs.getLong("authorId"),
                    rs.getLong("messageId"),
                    rs.getString("content"),
                    "",
                    emptyList(),
                    rs.getLong("moment")
                )
                results.add(result)
                if (results.size >= 100) {
                    context.daoManager.messageHistoryWrapper.addMessages(results)
                    count += 100
                    println("Progress: $count messages migrated")
                    results.clear()
                }
            }
        })
    }
}
