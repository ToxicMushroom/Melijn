package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer.BAND_COUNT
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetBandCommand : AbstractCommand("command.setband") {

    init {
        id = 112
        name = "setBand"
        aliases = arrayOf("setGain", "setBands")
        commandCategory = CommandCategory.MUSIC
        children = arrayOf(
            AllArg(root)
        )
        runConditions = arrayOf(
            RunCondition.VC_BOT_ALONE_OR_USER_DJ,
            RunCondition.PLAYING_TRACK_NOT_NULL,
            RunCondition.VOTED
        )
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val bandId = getIntegerFromArgNMessage(context, 0, 0, BAND_COUNT - 1) ?: return

        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        if (context.args.size == 1) {
            val bandVal = iPlayer.bands.getOrElse(bandId) { 0.0f }
            val gain = ((bandVal + 0.25f) * 400).toInt()

            val msg = context.getTranslation("$root.show")
                .replace("%bandId%", bandId.toString())
                .replace("%gain%", gain.toString())
            sendMsg(context, msg)
            return
        }
        val gain = getIntegerFromArgNMessage(context, 1, 0, 500) ?: return

        // Actual range is -0.25f to 1.0f
        val actualGain = (gain / 400f - 0.25f)
        iPlayer.setBand(bandId, actualGain)

        val msg = context.getTranslation("$root.set")
            .replace("%bandId%", bandId.toString())
            .replace("%gain%", gain.toString())
        sendMsg(context, msg)
    }

    class AllArg(parent: String) : AbstractCommand("$parent.all") {

        init {
            name = "all"
        }

        override suspend fun execute(context: CommandContext) {
            val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
            if (context.args.isEmpty()) {
                val title = context.getTranslation("$root.show")

                var content = "```INI"
                for (i in 0..14) {
                    val bandVal = iPlayer.bands.getOrElse(i) { 0.0f }
                    val gain = ((bandVal + 0.25f) * 400).toInt()
                    content += "\n[$i] - $gain%"
                }
                content += "```"

                val msg = title + content
                sendMsg(context, msg)

            } else {
                val gain = getIntegerFromArgNMessage(context, 0, 0, 500) ?: return
                val actualGain = (gain / 400f - 0.25f)

                for (i in 0..14) {
                    iPlayer.setBand(i, actualGain)
                }

                val msg = context.getTranslation("$root.set")
                    .replace("%gain%", gain.toString())
                sendMsg(context, msg)
            }
        }
    }
}