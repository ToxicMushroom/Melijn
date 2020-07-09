package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.ImageUtils
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
        ImageCommandUtil.executeNormalRecolorSingleOffset(context) { ints ->
            val value = ImageUtils.getBrightness(ints[0], ints[1], ints[2])
            intArrayOf(value, value, value, ints[3])
        }
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifRecolorSingleOffset(context, { ints ->
            var value = ImageUtils.getBrightness(ints[0], ints[1], ints[2])
            if (value == 255) {
                value = 254
            }
            if (ints[3] < 128) intArrayOf(255, 255, 255, 255)
            else intArrayOf(value, value, value, 255)
        }, false)
    }
}