package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission

class GreyScaleCommand : AbstractCommand("command.greyscale") {

    init {
        id = 128
        name = "greyscale"
        aliases = arrayOf("greyScaleGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("greyscaleGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalRecolor(context, { ints ->
            val value = ImageUtils.getBrightness(ints[0], ints[1], ints[2])
            arrayOf(value, value, value).toIntArray()
        }, false)
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifRecolor(context, { ints ->
            val value = ImageUtils.getBrightness(ints[0], ints[1], ints[2])
            arrayOf(value, value, value).toIntArray()
        }, false)
    }
}