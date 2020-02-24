package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.enums.GainType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_GAINTYPE
import me.melijn.melijnbot.objects.utils.getEnumFromArgN
import me.melijn.melijnbot.objects.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class LoadGainProfileCommand : AbstractCommand("command.loadgainprofile") {

    init {
        id = 141
        name = "loadGainProfile"
        aliases = arrayOf("lgp")
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val gainType = getEnumFromArgNMessage<GainType>(context, 0, MESSAGE_UNKNOWN_GAINTYPE) ?: return
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
        iPlayer.setBand(0, gainType.band0)
        iPlayer.setBand(1, gainType.band1)
        iPlayer.setBand(2, gainType.band2)
        iPlayer.setBand(3, gainType.band3)
        iPlayer.setBand(4, gainType.band4)
        iPlayer.setBand(5, gainType.band5)
        iPlayer.setBand(6, gainType.band6)
        iPlayer.setBand(7, gainType.band7)
        iPlayer.setBand(8, gainType.band8)
        iPlayer.setBand(9, gainType.band9)
        iPlayer.setBand(10, gainType.band10)
        iPlayer.setBand(11, gainType.band11)
        iPlayer.setBand(12, gainType.band12)
        iPlayer.setBand(13, gainType.band13)
        iPlayer.setBand(14, gainType.band14)

        val msg = context.getTranslation("$root.loaded")
            .replace("%gainType%", gainType.toString())
        sendMsg(context, msg)
    }
}