package me.melijn.melijnbot.commands.music

import kotlinx.coroutines.sync.withPermit
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.services.voice.VOICE_SAFE
import me.melijn.melijnbot.internals.utils.message.sendRsp

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
        VOICE_SAFE.withPermit {
            guildMusicPlayer.guildTrackManager.stopAndDestroy()
        }

        val msg = context.getTranslation("$root.success")
        sendRsp(context, msg)
    }
}