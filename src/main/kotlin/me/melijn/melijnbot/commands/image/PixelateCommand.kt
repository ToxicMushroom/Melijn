package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.filter.PixelateFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
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
        val blockSize = if (context.args.size > 1) getIntegerFromArgNMessage(context, 1, 1, 50) ?: return else 1
        if (image.type == ImageType.GIF) {
            pixelateGif(context, image, blockSize)
        } else {
            pixelateNormal(context, image, blockSize)
        }
    }

    private suspend fun pixelateNormal(context: ICommandContext, image: ParsedImageByteArray, blockSize: Int) {
        ImageCommandUtil.applyImmutableImgModification(context, image, { img ->
            PixelateFilter(blockSize).apply(img)
        })
    }

    private suspend fun pixelateGif(context: ICommandContext, image: ParsedImageByteArray, blockSize: Int) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, { img ->
            PixelateFilter(blockSize).apply(img)
        })
    }
}