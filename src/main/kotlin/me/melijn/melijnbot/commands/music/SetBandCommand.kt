package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer.BAND_COUNT
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getFloatFromArgNMessage
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SetBandCommand : AbstractCommand("command.setband") {

    init {
        id = 112
        name = "setBand"
        aliases = arrayOf("setGain")
        commandCategory = CommandCategory.MUSIC
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
    }

    override suspend fun execute(context: CommandContext) {
        val bandId = getIntegerFromArgNMessage(context, 0, 0, BAND_COUNT - 1) ?: return
        val gain = getFloatFromArgNMessage(context, 1, -0.25f, 1f) ?: return
        context.guildMusicPlayer.guildTrackManager.iPlayer.setBand(bandId, gain)

        val msg = context.getTranslation("$root.response")
            .replace("%bandId%", bandId.toString())
            .replace("%gain%", gain.toString())
        sendMsg(context, msg)
    }
}