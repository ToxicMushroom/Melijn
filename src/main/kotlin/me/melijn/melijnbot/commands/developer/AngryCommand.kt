package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class AngryCommand : AbstractCommand("command.angry") {

    init {
        id = 168
        name = "angry"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendRsp(context, ">angry <serverIdToMakeLeave aka destroy >:) (I hope)>")
            return
        }
        val guildId = getLongFromArgNMessage(context, 0) ?: return

        context.lavaManager.closeConnectionForced(guildId)
        sendRsp(context, "Closed connection: Angry $guildId")
    }
}