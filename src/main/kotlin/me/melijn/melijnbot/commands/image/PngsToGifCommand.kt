package me.melijn.melijnbot.commands.image

import com.squareup.gifencoder.*
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.getLongFromArgN
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class PngsToGifCommand : AbstractCommand("command.pngstogif") {

    init {
        id = 171
        name = "pngsToGif"
        aliases = arrayOf("ptg")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        val triple = ImageUtils.getImagesBytesNMessage(context, "png") ?: return

        ByteArrayOutputStream().use { baos ->
            val argOffset = if (triple.third) 1 else 0
            val loopAmount = getIntegerFromArgN(context, argOffset) ?: 0
            val frameDelay = getLongFromArgN(context, argOffset + 1, 0) ?: 100

            val maxWidth = triple.second.first
            val maxHeight = triple.second.second
            val gifEncoder = GifEncoder(baos, maxWidth, maxHeight, loopAmount)

            for ((_, f) in triple.first.toSortedMap()) {
                val frame = ByteArrayInputStream(f).use {
                    ImageIO.read(it)
                }

                val width = frame.width
                val height = frame.height

                ImageUtils.recolorPixelSingleOffset(frame, colorPicker = { ints ->
                    if (ints[0] == 255 && ints[1] == 255 && ints[2] == 255) {
                        intArrayOf(254, 254, 254, 255)
                    } else if (ints[3] < 128) {
                        intArrayOf(255, 255, 255, 255)
                    } else ints
                })

                val options = ImageOptions()
                    .setColorQuantizer(MedianCutQuantizer.INSTANCE)
                    .setDitherer(FloydSteinbergDitherer.INSTANCE)
                    .setTransparencyColor(Color.WHITE.rgb)
                    .setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE)
                    .setDelay(frameDelay, TimeUnit.MILLISECONDS)

                val img = frame.getRGB(0, 0, width, height, IntArray(width * height), 0, width)
                gifEncoder.addImage(img, width, options)
            }

            gifEncoder.finishEncoding()
            sendFileRsp(context, baos.toByteArray(), "gif")
        }
    }
}