package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.enums.GainType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_GAINTYPE
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
        val gainProfile = gainType.gainProfile
        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer

        iPlayer.setBand(0, gainProfile.band0)
        iPlayer.setBand(1, gainProfile.band1)
        iPlayer.setBand(2, gainProfile.band2)
        iPlayer.setBand(3, gainProfile.band3)
        iPlayer.setBand(4, gainProfile.band4)
        iPlayer.setBand(5, gainProfile.band5)
        iPlayer.setBand(6, gainProfile.band6)
        iPlayer.setBand(7, gainProfile.band7)
        iPlayer.setBand(8, gainProfile.band8)
        iPlayer.setBand(9, gainProfile.band9)
        iPlayer.setBand(10, gainProfile.band10)
        iPlayer.setBand(11, gainProfile.band11)
        iPlayer.setBand(12, gainProfile.band12)
        iPlayer.setBand(13, gainProfile.band13)
        iPlayer.setBand(14, gainProfile.band14)

        val msg = context.getTranslation("$root.loaded")
            .replace("%gainType%", gainType.toString())
        sendMsg(context, msg)
    }
}