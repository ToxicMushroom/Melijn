package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class NightcoreCommand : AbstractCommand("command.nightcore") {

    init {
        id = 196
        name = "nightcore"
        aliases = arrayOf("nc")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }


    override suspend fun execute(context: CommandContext) {
        val state = getBooleanFromArgNMessage(context, 0) ?: return

        if (state) {
            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            player.setRate(1.25)

            val msg = context.getTranslation("$root.enabled")
            sendRsp(context, msg)

        } else {

            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            player.setRate(1.0)

            val msg = context.getTranslation("$root.disabled")
            sendRsp(context, msg)
        }
    }
}