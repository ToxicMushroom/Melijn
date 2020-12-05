package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp

class MusicNodeCommand : AbstractCommand("command.musicnode") {

    init {
        id = 233
        name = "musicNode"
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        sendRsp(
            context,
            "" + context.lavaManager.jdaLavaLink?.getExistingLink(context.guildId)?.getNode()?.remoteUri?.port
        )
    }
}