package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
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
            flipFrame(image)
        }, false)
    }

    private fun flipFrame(image: BufferedImage) {
        val newImage = BufferedImage(image.width, image.height, image.type)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val color = image.getRGB(x, image.height - 1 - y)
                newImage.setRGB(x, y, color)
            }
        }
        for (y in 0 until newImage.height) {
            for (x in 0 until newImage.width) {
                val color = newImage.getRGB(x, y)
                image.setRGB(x, y, color)
            }
        }
    }
}