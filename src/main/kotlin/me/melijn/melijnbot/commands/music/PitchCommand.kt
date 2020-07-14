package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class PitchCommand : AbstractCommand("command.pitch") {

    init {
        id = 184
        name = "pitch"
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.getGuildMusicPlayer().guildTrackManager.iPlayer
        if (context.args.isEmpty()) {
            val msg = context.getTranslation("$root.show")
                .withVariable("pitch", iPlayer.pitch * 100)
            sendRsp(context, msg)
            return
        }

        val pitch = getLongFromArgNMessage(context, 0, 0, ignore = *arrayOf("%")) ?: return
        iPlayer.setPitch(pitch / 100.0)

        val msg = context.getTranslation("$root.set")
            .withVariable("pitch", pitch)
        sendRsp(context, msg)
        return
    }
}