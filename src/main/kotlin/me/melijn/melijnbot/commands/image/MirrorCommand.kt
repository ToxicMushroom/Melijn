package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import net.dv8tion.jda.api.Permission
import java.awt.image.BufferedImage

class MirrorCommand : AbstractCommand("command.mirror") {

    init {
        id = 130
        name = "mirror"
        aliases = arrayOf("mirrorGif")
        discordPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("mirrorGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, { image, _ ->
            mirrorFrame(image)
        }, false)
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, { image, _ ->
            mirrorFrame(image)
        }, false)
    }

    private fun mirrorFrame(image: BufferedImage) {
        val newImage = BufferedImage(image.width, image.height, image.type)
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val color = image.getRGB(image.width - 1 - x, y)
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