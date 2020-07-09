package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp

class ShuffleCommand : AbstractCommand("command.shuffle") {

    init {
        id = 86
        name = "shuffle"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        context.guildMusicPlayer.guildTrackManager.shuffle()
        val msg = context.getTranslation("$root.success")
        sendRsp(context, msg)
    }
}