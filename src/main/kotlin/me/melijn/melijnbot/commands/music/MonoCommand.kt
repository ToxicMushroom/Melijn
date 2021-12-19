package me.melijn.melijnbot.commands.music

import me.melijn.llklient.io.filters.ChannelMix
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp

class MonoCommand : AbstractCommand("command.mono") {

    init {
        name = "mono"
        runConditions =
            arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            val msg = if (player.filters.channelMix == null) {
                player.filters.channelMix = ChannelMix(1f, 1f, 1f, 1f)
                context.getTranslation("$root.enabled")
            } else {
                player.filters.channelMix = null
                context.getTranslation("$root.disabled")
            }

            player.filters.commit()
            sendRsp(context, msg)
            return
        }
    }
}