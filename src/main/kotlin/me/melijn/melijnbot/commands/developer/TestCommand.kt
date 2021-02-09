package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
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
        val user = retrieveUserByArgsNMessage(context, 0) ?: return


        sendMsg(context, "hi " + user.asTag)
    }
}
