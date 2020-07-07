package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.isPremiumUser
import me.melijn.melijnbot.objects.utils.message.sendFeatureRequiresPremiumMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.withVariable

const val VOLUME_OVER_150 = "premium.feature.volume.over.150"

class VolumeCommand : AbstractCommand("command.volume") {

    init {
        id = 87
        name = "volume"
        aliases = arrayOf("vol")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        if (context.args.isEmpty()) {
            val amount = iPlayer.volume
            val msg = context.getTranslation("$root.show")
                .withVariable("volume", amount.toString())
            sendRsp(context, msg)
            return
        }

        val amount = getIntegerFromArgNMessage(context, 0, 0, 1000) ?: return
        if (amount > 150 && !isPremiumUser(context)) {
            sendFeatureRequiresPremiumMessage(context, VOLUME_OVER_150)
            return
        }

        iPlayer.volume = amount

        val msg = context.getTranslation("$root.set")
            .withVariable("volume", amount.toString())
        sendRsp(context, msg)
    }
}