package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission

class SpookifyCommand : AbstractCommand("command.spookify") {

    init {
        id = 55
        name = "spookify"
        aliases = arrayOf("spookifyGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("spookifyGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalRecolorSingleOffset(context) { ints ->
            ImageUtils.getSpookyForPixel(ints[0], ints[1], ints[2], ints[3], ints[4])
        }
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifRecolorSingleOffset(context, { ints ->
            ImageUtils.getSpookyForPixel(ints[0], ints[1], ints[2], ints[3], ints[4], true)
        }, false)
    }
}