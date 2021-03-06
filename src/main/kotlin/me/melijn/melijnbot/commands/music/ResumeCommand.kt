package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.message.sendRsp

class ResumeCommand : AbstractCommand("command.resume") {

    init {
        id = 85
        name = "resume"
        aliases = arrayOf("unpause")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager

        //Adds a message to the resumeEventMessageQueue thing so it gets logged to MUSIC logchannel
        LogUtils.addMusicPlayerResumed(context)
        trackManager.setPaused(false)

        val msg = context.getTranslation("$root.success")
        sendRsp(context, msg)
    }
}