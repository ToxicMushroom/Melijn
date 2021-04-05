package me.melijn.melijnbot.commands.music

import me.melijn.llklient.io.filters.Timescale
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class RateCommand : AbstractCommand("command.rate") {

    init {
        id = 185
        name = "rate"
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.MUSIC
    }

    suspend fun execute(context: ICommandContext) {
        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        if (context.args.isEmpty()) {
            val currentRatePercent = (iPlayer.filters.timescale?.rate ?: 1.0f) * 100
            val msg = context.getTranslation("$root.show")
                .withVariable("rate", currentRatePercent * 100)
            sendRsp(context, msg)
            return
        }

        val rate = getLongFromArgNMessage(context, 0, 0, ignore = arrayOf("%")) ?: return
        val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer

        val ts = player.filters.timescale ?: Timescale()
        ts.rate = rate / 100.0f
        player.filters.timescale = ts
        player.filters.commit()

        val msg = context.getTranslation("$root.set")
            .withVariable("rate", rate)
        sendRsp(context, msg)
        return
    }
}