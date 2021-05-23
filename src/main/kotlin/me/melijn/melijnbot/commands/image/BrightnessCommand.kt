package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.BrightnessFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import me.melijn.melijnbot.internals.utils.getFloatFromArgNMessage
import net.dv8tion.jda.api.Permission

class BrightnessCommand : AbstractCommand("command.brightness") {

    init {
        name = "brightness"
        aliases = arrayOf("brightnessGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val brightness = getFloatFromArgNMessage(context, 1) ?: return
        if (image.type == ImageType.GIF) {
            brightnessGif(context, image, brightness)
        } else {
            brightnessNormal(context, image, brightness)
        }
    }

    private val modification: (brightness: Float) -> ((img: ImmutableImage) -> Unit) = { brightness ->
        { img ->
            BrightnessFilter(brightness).apply(img)
        }
    }

    private suspend fun brightnessNormal(context: ICommandContext, image: ParsedImageByteArray, brightness: Float) {
        ImageCommandUtil.applyImmutableImgModification(context, image, modification(brightness))
    }

    private suspend fun brightnessGif(context: ICommandContext, image: ParsedImageByteArray, brightness: Float) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification(brightness))
    }
}