package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.message.sendRsp

class StopCommand : AbstractCommand("command.stop") {

    init {
        id = 81
        name = "stop"
        aliases = arrayOf("leave", "disconnect")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ)
        commandCategory = CommandCategory.MUSIC
    }

    suspend fun execute(context: ICommandContext) {
        val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)
        guildMusicPlayer.guildTrackManager.clear()
        guildMusicPlayer.guildTrackManager.stopAndDestroy()

        val msg = context.getTranslation("$root.success")
        sendRsp(context, msg)
    }
}