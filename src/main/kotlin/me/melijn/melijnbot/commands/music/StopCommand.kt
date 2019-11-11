package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.sendMsg

class StopCommand : AbstractCommand("command.stop") {

    init {
        id = 81
        name = "stop"
        aliases = arrayOf("leave", "disconnect")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)
        guildMusicPlayer.guildTrackManager.clear()
        guildMusicPlayer.guildTrackManager.stop()
        context.lavaManager.closeConnection(context.guild)

        val msg = context.getTranslation("$root.success")
        sendMsg(context, msg)
    }
}