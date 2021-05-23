package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import net.dv8tion.jda.api.Permission
import java.awt.Color

class MirrorCommand : AbstractCommand("command.mirror") {

    init {
        id = 130
        name = "mirror"
        aliases = arrayOf("flipXGif", "flipXImg", "mirrorGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    private val modification: (img: ImmutableImage) -> Unit = { img ->
        val og = img.copy()
        img.mapInPlace {
            val pixel = og.pixel(img.width - it.x - 1, it.y)
            Color(pixel.argb, true)
        }
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) {
            ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification)
        } else {
            ImageCommandUtil.applyImmutableImgModification(context, image, modification)
        }
    }
}