package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission
import java.awt.image.BufferedImage

class FlipCommand : AbstractCommand("command.flip") {

    init {
        id = 129
        name = "flip"
        aliases = arrayOf("flipGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("flipGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, { image, _ ->
            flipFrame(image)
        }, false)
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, { image, _ ->
            flipFrame(image, true)
        }, false)
    }

    private fun flipFrame(image: BufferedImage, isGif: Boolean = false) {
        for (y in 0 until image.height / 2) {
            for (x in 0 until image.width) {
                var topColor = image.getRGB(x, y)
                var bottomColor = image.getRGB(x, image.height - y - 1)

                if (isGif) {
                    topColor = ImageUtils.suiteColorForGif(topColor)
                    bottomColor = ImageUtils.suiteColorForGif(bottomColor)
                }

                image.setRGB(x, y, bottomColor)
                image.setRGB(x, image.height - y - 1, topColor)
            }
        }
    }
}
