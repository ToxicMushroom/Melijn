package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import net.dv8tion.jda.api.Permission
import java.awt.Color

class FlipImgCommand : AbstractCommand("command.flipimg") {

    init {
        id = 129
        name = "flipImg"
        aliases = arrayOf("flipY", "flipGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) {
            flipGif(context, image)
        } else {
            flipNormal(context, image)
        }
    }

    private val modification: (img: ImmutableImage) -> Unit = { img ->
        val og = img.copy()
        img.mapInPlace {
            val pixel = og.pixel(it.x, img.height - 1 - it.y)
            Color(pixel.argb, true)
        }
    }

    private suspend fun flipNormal(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyImmutableImgModification(context, image, modification)
    }

    private suspend fun flipGif(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification)
    }
}
