package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class VolumeCommand : AbstractCommand("command.volume") {

    init {
        id = 87
        name = "volume"
        aliases = arrayOf("vol")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            val amount = context.getGuildMusicPlayer().guildTrackManager.iPlayer.volume
            val msg = i18n.getTranslation(context, "$root.show")
                .replace("%volume%", amount.toString())
            sendMsg(context, msg)
            return
        }
        val amount = getIntegerFromArgNMessage(context, 0, 0, 1000) ?: return
        context.getGuildMusicPlayer().guildTrackManager.iPlayer.volume = amount

        val msg = i18n.getTranslation(context, "$root.set")
            .replace("%volume%", amount.toString())
        sendMsg(context, msg)
    }
}