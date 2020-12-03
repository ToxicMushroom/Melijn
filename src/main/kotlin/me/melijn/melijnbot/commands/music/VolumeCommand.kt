package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.isPremiumUser
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

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
        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        if (context.args.isEmpty()) {
            val amount = iPlayer.filters.volume
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

        iPlayer.filters.volume = amount
        iPlayer.filters.commit()

        val msg = context.getTranslation("$root.set")
            .withVariable("volume", amount.toString())
        sendRsp(context, msg)
    }
}