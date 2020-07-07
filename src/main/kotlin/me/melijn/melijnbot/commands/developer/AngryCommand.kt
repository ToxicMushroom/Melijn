package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getLongFromArgNMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp

class AngryCommand : AbstractCommand("command.angry") {

    init {
        id = 168
        name = "angry"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendRsp(context, ">angry <serverIdToMakeLeave aka destroy >:) (I hope)>")
            return
        }
        val guildId = getLongFromArgNMessage(context, 0) ?: return
        context.lavaManager.closeConnectionAngry(guildId, context.daoManager.musicNodeWrapper.isPremium(guildId))
        sendRsp(context, "Closed connection: Angry $guildId")
    }
}