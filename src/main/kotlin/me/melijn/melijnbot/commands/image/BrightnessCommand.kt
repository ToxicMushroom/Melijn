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
import me.melijn.melijnbot.internals.utils.getFloatFromArgNMessage
import me.melijn.melijnbot.internals.utils.plus
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
        val brightness = getFloatFromArgNMessage(context, image.usedArgument + 0) ?: return
        if (image.type == ImageType.GIF) {
            ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification(brightness))
        } else {
            ImageCommandUtil.applyImmutableImgModification(context, image, modification(brightness))
        }
    }

    private val modification: (brightness: Float) -> ((img: ImmutableImage) -> Unit) = { brightness ->
        { img ->
            BrightnessFilter(brightness).apply(img)
        }
    }
}