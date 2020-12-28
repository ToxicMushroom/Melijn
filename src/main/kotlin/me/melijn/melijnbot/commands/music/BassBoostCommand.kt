package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.database.audio.GainProfile
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getBooleanFromArgN
import me.melijn.melijnbot.internals.utils.getLongFromArgN
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable


class BassBoostCommand : AbstractCommand("command.bassboost") {

    init {
        id = 195
        name = "bassBoost"
        aliases = arrayOf("bb")
        runConditions =
            arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }


    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            val (band1, band2) = player.filters.bands
            val (bassBand1, bassBand2) = BASE_BASS
            val msg = if (band1 / bassBand1 == band2 / bassBand2) {
                context.getTranslation("$root.show")
                    .withVariable("value", band1 / bassBand1 * 100.0f)

            } else {
                context.getTranslation("$root.custombands")
            }
            sendRsp(context, msg)
            return
        }

        val state = getBooleanFromArgN(context, 0)
        val amount = getLongFromArgN(context, 0, 0, 500, ignore = arrayOf("%"))

        val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        val msg = when {
            amount != null -> {
                val band0 = (amount / 100.0f * BASE_BASS.band0)
                val band1 = (amount / 100.0f * BASE_BASS.band1)
                player.filters.setBand(0, band0)
                player.filters.setBand(1, band1)

                context.getTranslation("$root.set")
                    .withVariable("value", amount)
            }
            state != null -> {
                if (state) {
                    player.filters.setBand(0, BASE_BASS.band0)
                    player.filters.setBand(1, BASE_BASS.band1)
                    context.getTranslation("$root.enabled")
                } else {
                    player.filters.setBand(0, 0.0f)
                    player.filters.setBand(1, 0.0f)
                    context.getTranslation("$root.disabled")
                }
            }
            else -> {
                sendSyntax(context)
                return
            }
        }
        player.filters.commit()
        sendRsp(context, msg)
    }

    companion object {
        val customBass = mutableMapOf<Long, Int>()
        val BASE_BASS = GainProfile(0.20f, 0.10f)
    }
}
