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
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument + 0

        val color = getColorFromArgNMessage(context, offset) ?: return
        var target = getColorFromArgNMessage(context, offset + 1) ?: return
        val srcAlpha = context.optional(offset + 2, 255) { getIntegerFromArgNMessage(context, it, -1, 255) } ?: return
        val alpha = context.optional(offset + 3, 255) { getIntegerFromArgNMessage(context, it, 0, 255) } ?: return
        target = Color(target.red, target.green, target.blue, alpha)

        if (image.type == ImageType.GIF) {
            ImageCommandUtil.applyGifImmutableFrameModification(context, image, modification(color, srcAlpha, target))
        } else {
            ImageCommandUtil.applyImmutableImgModification(context, image, modification(color, srcAlpha, target))
        }
    }

    private val modification: (src: Color, srcAlpha: Int, target: Color) -> ((img: ImmutableImage) -> Unit) =
        { src, srcAlpha, target ->
            { img ->
                img.mapInPlace {
                    if (
                        it.red() == src.red &&
                        it.green() == src.green &&
                        it.blue() == src.blue &&
                        (srcAlpha == -1 || src.alpha == srcAlpha)
                    ) {
                        target
                    } else Color(it.argb, true)
                }
            }
        }
}