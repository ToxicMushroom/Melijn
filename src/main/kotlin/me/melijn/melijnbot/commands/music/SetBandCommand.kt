package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer.BAND_COUNT
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class SetBandCommand : AbstractCommand("command.setband") {

    init {
        id = 112
        name = "setBand"
        aliases = arrayOf("setGain", "setBands", "sb")
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

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val bandId = getIntegerFromArgNMessage(context, 0, 0, BAND_COUNT - 1) ?: return

        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        if (context.args.size == 1) {
            val bandVal = iPlayer.filters.bands.getOrElse(bandId) { 0.0f }
            val gain = ((bandVal + 0.25f) * 400).toInt()

            val msg = context.getTranslation("$root.show")
                .withVariable("bandId", bandId.toString())
                .withVariable("gain", gain.toString())
            sendRsp(context, msg)
            return
        }
        val gain = getIntegerFromArgNMessage(context, 1, 0, 500) ?: return

        // Actual range is -0.25f to 1.0f
        val actualGain = (gain / 400f - 0.25f)
        iPlayer.filters.setBand(bandId, actualGain)
        iPlayer.filters.commit()

        val msg = context.getTranslation("$root.set")
            .withVariable("bandId", bandId.toString())
            .withVariable("gain", gain.toString())
        sendRsp(context, msg)
    }

    class AllArg(parent: String) : AbstractCommand("$parent.all") {

        init {
            name = "all"
        }

        override suspend fun execute(context: ICommandContext) {
            val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            if (context.args.isEmpty()) {
                val title = context.getTranslation("$root.show")

                var content = "```INI"
                for (i in 0..14) {
                    val bandVal = iPlayer.filters.bands.getOrElse(i) { 0.0f }
                    val gain = ((bandVal + 0.25f) * 400).toInt()
                    content += "\n[$i] - $gain%"
                }
                content += "```"

                val msg = title + content
                sendRsp(context, msg)

            } else {
                val gain = getIntegerFromArgNMessage(context, 0, 0, 500) ?: return
                val actualGain = (gain / 400f - 0.25f)

                for (i in 0..14) {
                    iPlayer.filters.setBand(i, actualGain)
                }
                iPlayer.filters.commit()

                val msg = context.getTranslation("$root.set")
                    .withVariable("gain", gain.toString())
                sendRsp(context, msg)
            }
        }
    }
}