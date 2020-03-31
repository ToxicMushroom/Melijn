package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission
import java.lang.Integer.max

class PixelateCommand : AbstractCommand("command.pixelate") {

    init {
        id = 133
        name = "pixelate"
        aliases = arrayOf("pixelateGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("pixelateGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, i ->
            ImageUtils.pixelate(image, i)

        }, hasOffset = true, defaultOffset = { img ->
            max(1, max(img.height, img.width) / 100)

        }, offsetRange = { img ->
            IntRange(1, max(img.height, img.width))

        })
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, i ->
            ImageUtils.pixelate(image, i, true)

        }, hasOffset = true, defaultOffset = { img ->
            max(1, max(img.height, img.width) / 100)

        }, offsetRange = { img ->
            IntRange(1, max(img.height, img.width))

        }, debug = false)
    }
}