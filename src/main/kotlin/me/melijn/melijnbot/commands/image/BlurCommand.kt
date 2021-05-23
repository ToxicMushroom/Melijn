package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission
import thirdparty.jhlabs.image.BoxBlurFilter
import java.awt.image.BufferedImage

class BlurCommand : AbstractCommand("command.blur") {

    init {
        id = 135
        name = "blur"
        aliases = arrayOf("blurGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        cooldown = 5000
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument + 0
        val intensity = context.optional(offset, 1) { getIntegerFromArgNMessage(context, it, 1, 10) } ?: return
        if (image.type == ImageType.GIF) {
            ImageCommandUtil.applyGifBufferFrameModification(context, image, modifications(intensity))
        } else {
            ImageCommandUtil.applyBufferedImgModification(context, image, modifications(intensity))
        }
    }

    val modifications: (intensity: Int) -> ((src: BufferedImage, dst: BufferedImage) -> Unit) = { intensity ->
        { src, dst ->
            val res = ((src.width + src.height) / 100).toFloat() * intensity
            BoxBlurFilter(res, res, 1).filter(src, dst)
        }
    }
}