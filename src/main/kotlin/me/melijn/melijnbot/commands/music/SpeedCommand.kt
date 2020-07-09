package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class SpeedCommand : AbstractCommand("command.speed") {

    init {
        id = 183
        name = "speed"
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        if (context.args.isEmpty()) {
            val msg = context.getTranslation("$root.show")
                .withVariable("speed", iPlayer.speed * 100)
            sendRsp(context, msg)
            return
        }

        val speed = getLongFromArgNMessage(context, 0, 0, ignore = *arrayOf("%")) ?: return
        iPlayer.speed = speed / 100.0

        val msg = context.getTranslation("$root.set")
            .withVariable("speed", speed)
        sendRsp(context, msg)
        return
    }
}