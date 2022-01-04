package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import me.melijn.melijnbot.internals.utils.optional
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PngsToGifCommand : AbstractCommand("command.pngstogif") {

    init {
        id = 171
        name = "pngsToGif"
        aliases = arrayOf("ptg")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val parsedImages = ImageUtils.getImagesBytesNMessage(context, 0) ?: return

        val argOffset = if (parsedImages.usedArgument) 1 else 0
        val iterations = context.optional(argOffset, 0) { getIntegerFromArgNMessage(context, argOffset) } ?: return
        val delay = context.optional(argOffset + 1, 300) { getIntegerFromArgNMessage(context, argOffset + 1) } ?: return

        val baos = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(baos).use { ios ->
            GifSequenceWriter(ios, iterations).use { writer ->
                for (frame in parsedImages.images) {
                    val immutableImage = ImmutableImage.loader()
                        .fromBytes(frame.bytes)
                    writer.writeToSequence(immutableImage.awt(), delay)
                }
            }
        }

        sendFileRsp(context, baos.toByteArray(), "gif")
    }
}