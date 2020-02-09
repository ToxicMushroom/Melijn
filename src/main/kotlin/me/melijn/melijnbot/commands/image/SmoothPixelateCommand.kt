package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission

class SmoothPixelateCommand : AbstractCommand("command.smoothpixelate") {

    init {
        id = 134
        name = "smoothPixelate"
        aliases = arrayOf("smoothPixelateGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("smoothPixelateGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, i ->
            ImageUtils.pixelate(image, i)

        }, hasOffset = true, defaultOffset = { img ->
            Integer.max(img.height, img.width) / 100

        }, offsetRange = { img ->
            IntRange(1, Integer.max(img.height, img.width))

        })
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, i ->
            ImageUtils.pixelate(image, i)

        }, hasOffset = true, defaultOffset = { img ->
            Integer.max(img.height, img.width) / 100

        }, offsetRange = { img ->
            IntRange(1, Integer.max(img.height, img.width))

        })
    }
}