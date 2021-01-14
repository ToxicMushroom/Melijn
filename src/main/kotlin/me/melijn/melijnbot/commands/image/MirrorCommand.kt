package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageUtils
import net.dv8tion.jda.api.Permission
import java.awt.image.BufferedImage

class MirrorCommand : AbstractCommand("command.mirror") {

    init {
        id = 130
        name = "mirror"
        aliases = arrayOf("mirrorGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.commandParts[1].equals("mirrorGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: ICommandContext) {
        ImageCommandUtil.executeNormalEffect(context, { image, _ ->
            mirrorFrame(image)
        })
    }

    private suspend fun executeGif(context: ICommandContext) {
        ImageCommandUtil.executeGifEffect(context, { image, _ ->
            mirrorFrame(image, true)
        })
    }

    private fun mirrorFrame(image: BufferedImage, isGif: Boolean = false) {
        for (y in 0 until image.height) {
            for (x in 0 until image.width / 2) {
                var leftColor = image.getRGB(x, y)
                var rightColor = image.getRGB(image.width - x - 1, y)

                if (isGif) {
                    leftColor = ImageUtils.suiteColorForGif(leftColor)
                    rightColor = ImageUtils.suiteColorForGif(rightColor)
                }

                image.setRGB(x, y, rightColor)
                image.setRGB(image.width - x - 1, y, leftColor)
            }
        }
    }
}