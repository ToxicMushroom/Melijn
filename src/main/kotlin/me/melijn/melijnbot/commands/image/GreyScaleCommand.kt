package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.GrayscaleFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import net.dv8tion.jda.api.Permission

class GreyScaleCommand : AbstractCommand("command.greyscale") {

    init {
        id = 128
        name = "greyscale"
        aliases = arrayOf("greyScaleGif", "grayscale")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) {
            greyscaleGif(context, image)
        } else {
            greyscaleNormal(context, image)
        }
    }

    private val modification: (img: ImmutableImage) -> Unit = { img ->
        GrayscaleFilter().apply(img)
    }

    private suspend fun greyscaleNormal(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyImmutableImgModification(context, image, modification)
    }

    private suspend fun greyscaleGif(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification)
    }
}