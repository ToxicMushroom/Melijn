package me.melijn.melijnbot.commands.music

import me.melijn.llklient.io.filters.Timescale
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getBooleanFromArgN
import me.melijn.melijnbot.internals.utils.getLongFromArgN
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class NightcoreCommand : AbstractCommand("command.nightcore") {

    init {
        id = 196
        name = "nightcore"
        aliases = arrayOf("nc")
        runConditions =
            arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }


    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            val ts = player.filters.timescale ?: Timescale()
            val nc = (ts.rate - 1.0f) * 100
            val msg = context.getTranslation("$root.show")
                .withVariable("value", nc.toInt())
            sendRsp(context, msg)
            return
        }

        val state = getBooleanFromArgN(context, 0)
        val amount = getLongFromArgN(context, 0, 0, 500, ignore = arrayOf("%"))

        val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        val ts = player.filters.timescale ?: Timescale()
        val msg = when {
            amount != null -> {
                ts.rate = (amount / 100.0f * 0.20f) + 1f
                context.getTranslation("$root.set")
                    .withVariable("value", amount)
            }
            state != null -> {
                if (state) {
                    ts.rate = 1.20f
                    context.getTranslation("$root.enabled")
                } else {
                    ts.rate = 1.0f
                    context.getTranslation("$root.disabled")
                }
            }
            else -> {
                sendSyntax(context)
                return
            }
        }
        player.filters.timescale = ts
        player.filters.commit()
        sendRsp(context, msg)
    }
}