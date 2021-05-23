package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import net.dv8tion.jda.api.Permission


class BlurpleCommand : AbstractCommand("command.blurple") {

    init {
        id = 54
        name = "blurple"
        aliases = arrayOf("blurpleGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = if (context.args.size > 1) {
            getIntegerFromArgNMessage(context, 1, -256, 256) ?: return
        } else 128
        if (image.type == ImageType.GIF) {
            blurpleGif(context, image, offset)
        } else {
            blurpleNormal(context, image, offset)
        }
    }

    private suspend fun blurpleNormal(context: ICommandContext, image: ParsedImageByteArray, offset: Int) {
        ImageCommandUtil.applyImmutableImgModification(context, image, { img ->
            img.mapInPlace { ImageUtils.getFakeBlurpleForPixel(it, offset, false) }
        })
    }

    private suspend fun blurpleGif(context: ICommandContext, image: ParsedImageByteArray, offset: Int) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, { img ->
            img.mapInPlace { ImageUtils.getFakeBlurpleForPixel(it, offset, true) }
        })
    }
}
