package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getLongFromArgNMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.withVariable

class PitchCommand : AbstractCommand("command.pitch") {

    init {
        id = 184
        name = "pitch"
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        if (context.args.isEmpty()) {
            val msg = context.getTranslation("$root.show")
                .withVariable("pitch", iPlayer.speed * 100)
            sendRsp(context, msg)
            return
        }

        val pitch = getLongFromArgNMessage(context, 0, 0, ignore = *arrayOf("%")) ?: return
        iPlayer.pitch = pitch / 100.0

        val msg = context.getTranslation("$root.set")
            .withVariable("pitch", pitch)
        sendRsp(context, msg)
        return
    }
}