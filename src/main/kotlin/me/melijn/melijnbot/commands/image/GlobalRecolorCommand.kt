package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import me.melijn.melijnbot.internals.utils.getColorFromArgNMessage
import net.dv8tion.jda.api.Permission
import java.awt.Color

class GlobalRecolorCommand : AbstractCommand("command.globalrecolor") {

    init {
        id = 154
        name = "globalRecolor"
        aliases = arrayOf("globalRecolorGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val color = getColorFromArgNMessage(context, 1) ?: return
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) {
            greyscaleGif(context, image, color)
        } else {
            greyscaleNormal(context, image, color)
        }
    }

    private val modification: (color: Color) -> ((img: ImmutableImage) -> Unit) = { color ->
        { img ->
            img.mapInPlace {
                Color(color.red, color.green, color.blue, it.alpha())
            }
        }
    }

    private suspend fun greyscaleNormal(context: ICommandContext, image: ParsedImageByteArray, color: Color) {
        ImageCommandUtil.applyImmutableImgModification(context, image, modification(color))
    }

    private suspend fun greyscaleGif(context: ICommandContext, image: ParsedImageByteArray, color: Color) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image,  modification(color))
    }
}