package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
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
        val iterations = getIntegerFromArgN(context, argOffset) ?: 0
        val delay = getIntegerFromArgN(context, argOffset + 1, 0) ?: 100

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