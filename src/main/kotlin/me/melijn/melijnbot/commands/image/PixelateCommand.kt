package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.filter.PixelateFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission

class PixelateCommand : AbstractCommand("command.pixelate") {

    init {
        id = 133
        name = "pixelate"
        aliases = arrayOf("pixelateGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument + 0
        val intensity = context.optional(offset, 4) { getIntegerFromArgNMessage(context, it, 1, 100) } ?: return
        if (image.type == ImageType.GIF) {
            pixelateGif(context, image, intensity)
        } else {
            pixelateNormal(context, image, intensity)
        }
    }

    private suspend fun pixelateNormal(context: ICommandContext, image: ParsedImageByteArray, blockSize: Int) {
        ImageCommandUtil.applyImmutableImgModification(context, image, { img ->
            val res = ((img.width + img.height) / 400).toFloat() * blockSize
            PixelateFilter(res.toInt()).apply(img)
        })
    }

    private suspend fun pixelateGif(context: ICommandContext, image: ParsedImageByteArray, blockSize: Int) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, { img ->
            val res = ((img.width + img.height) / 400).toFloat() * blockSize
            PixelateFilter(res.toInt()).apply(img)
        })
    }
}