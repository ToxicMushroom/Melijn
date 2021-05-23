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
import java.awt.image.BufferedImage
import javax.imageio.ImageIO


class JailCommand : AbstractCommand("command.jail") {

    init {
        id = 219
        name = "jail"
        aliases = arrayOf("jailGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    private val jail = JailCommand::class.java.getResourceAsStream("/jail.png").use {
        ImmutableImage.wrapAwt(ImageIO.read(it))
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return

        if (image.type == ImageType.GIF) {
            sharpenGif(context, image)
        } else {
            sharpenNormal(context, image)
        }
    }

    private suspend fun sharpenNormal(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyImmutableImgModification(context, image, { img ->
            val jailRaw = jail.scaleTo(img.width, img.height).awt()
            img.overlayInPlace(jailRaw, 0, 0)
        },  message = "**Go to jail** \uD83D\uDC6E\u200D♀️")
    }

    private suspend fun sharpenGif(context: ICommandContext, image: ParsedImageByteArray) {
        var jailRaw: BufferedImage? = null
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, { img ->
            val temp = jailRaw ?: run {
                jailRaw = jail.scaleTo(img.width, img.height).awt()
                jailRaw ?: throw StinkyException()
            }
            img.overlayInPlace(temp, 0, 0)
        }, message = "**Go to jail** \uD83D\uDC6E\u200D♀️")
    }
}
