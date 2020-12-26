package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    val logger: Logger = LoggerFactory.getLogger(TestCommand::class.java)

    override suspend fun execute(context: CommandContext) {
        context.daoManager.driverManager.executeUpdate(
            "ALTER TABLE public.forcedroles DROP CONSTRAINT forcedroles_pkey;\n" +
                "ALTER TABLE public.forcedroles ADD CONSTRAINT forcedroles_pkey PRIMARY KEY (guildid,userid,roleid);"
        )

        sendRsp(context, "Redid the pkey for forcedroles")
    }
}
