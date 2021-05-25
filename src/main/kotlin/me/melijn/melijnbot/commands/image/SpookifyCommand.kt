package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission

class SpookifyCommand : AbstractCommand("command.spookify") {

    init {
        id = 55
        name = "spookify"
        aliases = arrayOf("spookifyGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val argOffset = image.usedArgument + 0
        val offset = context.optional(argOffset, 128) { getIntegerFromArgNMessage(context, it, -256, 256) } ?: return
        if (image.type == ImageType.GIF) {
            ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification(offset))
        } else {
            ImageCommandUtil.applyImmutableImgModification(context, image, modification(offset))
        }
    }

    private val modification: (offset: Int) -> ((img: ImmutableImage) -> Unit) = { offset ->
        { img ->
            img.mapInPlace { ImageUtils.getSpookyForPixel(it, offset, false) }
        }
    }
}