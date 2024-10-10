package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission
import thirdparty.jhlabs.image.SharpenFilter
import java.awt.image.BufferedImage
import java.awt.image.Kernel

class SharpenCommand : AbstractCommand("command.sharpen") {

    init {
        id = 136
        name = "sharpen"
        aliases = arrayOf("sharpenGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf()
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument + 0
        val intensity = context.optional(offset, 1) { getIntegerFromArgNMessage(context, it, 1, 100) } ?: return
        if (image.type == ImageType.GIF) {
            sharpenGif(context, image, intensity)
        } else {
            sharpenNormal(context, image, intensity)
        }
    }

    val sharpener: (src: BufferedImage, dst: BufferedImage, intensity: Float) -> Unit = { src, dst, intensity ->
        val farr = floatArrayOf(
            0.0f, -0.2f - intensity, 0.0f,
            -0.2f - intensity, 1.8f + intensity * 4, -0.2f - intensity,
            0.0f, -0.2f - intensity, 0.0f
        )
        SharpenFilter().apply {
            kernel = Kernel(3, 3, farr)
        }.filter(src, dst)
    }

    private suspend fun sharpenNormal(context: ICommandContext, image: ParsedImageByteArray, intensity: Int) {
        ImageCommandUtil.applyBufferedImgModification(context, image) { src, dst ->
            sharpener(src, dst, (intensity - 1) / 10f)
        }
    }

    private suspend fun sharpenGif(context: ICommandContext, image: ParsedImageByteArray, intensity: Int) {
        ImageCommandUtil.applyGifBufferFrameModification(context, image) { src, dst ->
            sharpener(src, dst, (intensity - 1) / 10f)
        }
    }
}