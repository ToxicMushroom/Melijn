package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission
import java.awt.Color

class ReplaceColorCommand : AbstractCommand("command.replacecolor") {

    init {
        id = 173
        name = "replaceColor"
        aliases = arrayOf("replaceColorGif", "repc")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val color = getColorFromArgNMessage(context, 1) ?: return
        var target = getColorFromArgNMessage(context, 2) ?: return
        val alpha = if (context.args.size > 3) getIntegerFromArgNMessage(context, 3) ?: return else 255
        target = Color(target.red, target.green, target.blue, alpha)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) {
            replaceColorGif(context, image, color, target)
        } else {
            replaceColorNormal(context, image, color, target)
        }
    }

    private val modification: (src: Color, target: Color) -> ((img: ImmutableImage) -> Unit) = { src, target ->
        { img ->
            img.mapInPlace {
                if (
                    it.red() == src.red &&
                    it.green() == src.green &&
                    it.blue() == src.blue
                ) {
                    target
                } else Color(it.argb, true)
            }
        }
    }

    private suspend fun replaceColorNormal(
        context: ICommandContext,
        image: ParsedImageByteArray,
        color: Color,
        target: Color
    ) {
        ImageCommandUtil.applyImmutableImgModification(context, image, modification(color, target))
    }

    private suspend fun replaceColorGif(
        context: ICommandContext,
        image: ParsedImageByteArray,
        color: Color,
        target: Color
    ) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification(color, target))
    }
}