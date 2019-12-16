package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.LogUtils
import me.melijn.melijnbot.objects.utils.sendMsg

class PauseCommand : AbstractCommand("command.pause") {

    init {
        id = 84
        name = "pause"
        aliases = arrayOf("unresume")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager

        //Adds a message to the resumeEventMessageQueue thing so it gets logged to MUSIC logchannel
        LogUtils.addMusicPlayerPaused(context)
        trackManager.setPaused(true)

        val msg = context.getTranslation("$root.success")
        sendMsg(context, msg)
    }
}