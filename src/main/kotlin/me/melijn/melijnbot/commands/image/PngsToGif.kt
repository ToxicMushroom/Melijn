package me.melijn.melijnbot.commands.image

import com.squareup.gifencoder.*
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.getIntegerFromArgN
import me.melijn.melijnbot.objects.utils.getLongFromArgN
import me.melijn.melijnbot.objects.utils.sendFile
import java.awt.Color
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class PngsToGif : AbstractCommand("command.pngstogif") {

    init {
        id = 171
        name = "pngsToGif"
        aliases = arrayOf("ptg")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        val triple = ImageUtils.getImagesBytesNMessage(context, "png") ?: return


        val baos = ByteArrayOutputStream()

        val argoffset = if (triple.third) 1 else 0
        val loopAmount = getIntegerFromArgN(context, argoffset) ?: 0
        val frameDelay = getLongFromArgN(context, argoffset + 1, 0) ?: 100

        val maxwidth = triple.second.first
        val maxheight = triple.second.second
        val gifEncoder = GifEncoder(baos, maxwidth, maxheight, loopAmount)

        for (f in triple.first) {
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
            options.setColorQuantizer(MedianCutQuantizer.INSTANCE)
            options.setDitherer(FloydSteinbergDitherer.INSTANCE)
            options.setTransparencyColor(Color.WHITE.rgb)
            options.setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE)
            options.setDelay(frameDelay, TimeUnit.MILLISECONDS)
            val img = frame.getRGB(0, 0, width, height, IntArray(width * height), 0, width)
            gifEncoder.addImage(img, width, options)
        }
        gifEncoder.finishEncoding()
        sendFile(context, baos.toByteArray(), "gif")
    }
}