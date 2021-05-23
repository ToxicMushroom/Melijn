package me.melijn.melijnbot.commands.image

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
import thirdparty.jhlabs.image.BoxBlurFilter

class BlurCommand : AbstractCommand("command.blur") {

    init {
        id = 135
        name = "blur"
        aliases = arrayOf("blurGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        cooldown = 5000
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val intensity = if (context.args.size > 1) getIntegerFromArgNMessage(context, 1, 1, 10) ?: return else 1
        if (image.type == ImageType.GIF) {
            blurGif(context, image, intensity)
        } else {
            blurNormal(context, image, intensity)
        }
    }

    private suspend fun blurNormal(context: ICommandContext, image: ParsedImageByteArray, intensity: Int) {
        ImageCommandUtil.applyBufferedImgModification(context, image) { src, dst ->
            val res = ((src.width + src.height) / 100).toFloat() * intensity
            BoxBlurFilter(res, res, 1).filter(src, dst)
        }
    }

    private suspend fun blurGif(context: ICommandContext, image: ParsedImageByteArray, intensity: Int) {
        ImageCommandUtil.applyGifBufferFrameModification(context, image) { src, dst ->
            val res = ((src.width + src.height) / 100).toFloat() * intensity
            BoxBlurFilter(res, res, 1).filter(src, dst)
        }
    }
}