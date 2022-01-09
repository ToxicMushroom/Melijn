package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFile
import me.melijn.melijnbot.internals.utils.message.sendRspAwaitEL
import net.dv8tion.jda.api.entities.Message
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class GifCommand : AbstractCommand("command.gif") {

    init {
        name = "gif"
        aliases = arrayOf()
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.JPG, ImageType.TIFF, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument
        val repeatCount = context.optional(offset + 0, 0) { getIntegerFromArgNMessage(context, it, -1, 1000) } ?: return
        val centiSecondDelay =
            context.optional(offset + 1, -1) { getIntegerFromArgNMessage(context, it, -1, 1000) } ?: return

        if (image.type == ImageType.GIF) ImageCommandUtil.applyGifImmutableFrameModification(
            context,
            image,
            {},
            { delay ->
                if (centiSecondDelay == -1) delay
                else centiSecondDelay
            },
            repeatCount,
            "Rerendered gif"
        )
        else {
            val frame = ImmutableImage.loader()
                .fromBytes(image.bytes)
                .awt()
            val message = sendRspAwaitEL(context, "<a:loading:867394725938331658> Applying image effects")
            fun deleteLoadingMessage(message: List<Message>) {
                message.firstOrNull()?.delete()?.reason("Temporary status progress message")?.queue()
            }

            val baos = ByteArrayOutputStream()
            ImageIO.createImageOutputStream(baos).use { ios ->
                GifSequenceWriter(ios, repeatCount).use { gifWriter ->
                    gifWriter.writeToSequence(frame, centiSecondDelay)
                }
            }

            val temp = baos.toByteArray()
            deleteLoadingMessage(message)
            if (temp.size > Message.MAX_FILE_SIZE) {
                ImageCommandUtil.sendMsgMaxFilesizeHit(context, image)
                return
            }

            val ogSize = StringUtils.humanReadableByteCountBin(image.bytes.size)
            val newSize = StringUtils.humanReadableByteCountBin(temp.size)
            sendFile(
                context,
                "**$ogSize** -> **$newSize**",
                temp, "gif"
            )
        }
    }
}