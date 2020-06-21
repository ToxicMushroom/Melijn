package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getLongFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.withVariable

class RateCommand : AbstractCommand("command.rate") {

    init {
        id = 185
        name = "rate"
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        if (context.args.isEmpty()) {
            val msg = context.getTranslation("$root.show")
                .withVariable("rate", iPlayer.speed * 100)
            sendMsg(context, msg)
            return
        }

        val rate = getLongFromArgNMessage(context, 0, 0, ignore = *arrayOf("%")) ?: return
        iPlayer.rate = rate / 100.0

        val msg = context.getTranslation("$root.set")
            .withVariable("rate", rate)
        sendMsg(context, msg)
        return
    }
}