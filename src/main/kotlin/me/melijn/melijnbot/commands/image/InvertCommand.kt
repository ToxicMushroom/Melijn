package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.InvertFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import net.dv8tion.jda.api.Permission

class InvertCommand : AbstractCommand("command.invert") {

    init {
        id = 56
        name = "invert"
        aliases = arrayOf("invertGif", "inverse", "inverseGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) {
            invertGif(context, image)
        } else {
            invertNormal(context, image)
        }
    }

    private val modification: (img: ImmutableImage) -> Unit = { img ->
        InvertFilter().apply(img)
    }

    private suspend fun invertNormal(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyImmutableImgModification(context, image, modification)
    }

    private suspend fun invertGif(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification)
    }
}