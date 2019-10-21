package me.melijn.melijnbot.commandutil.image

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.getIntegerFromArgN
import me.melijn.melijnbot.objects.utils.sendFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

object ImageCommandUtil {


    suspend fun executeNormalRecolor(context: CommandContext, recolor: (ints: IntArray) -> IntArray, hasOffset: Boolean = true) {
        executeNormalEffect(context, { image, offset ->
            ImageUtils.recolorPixel(image, offset) { ints ->
                recolor(ints)
            }
        }, hasOffset)
    }

    suspend fun executeNormalEffect(context: CommandContext, effect: (image: BufferedImage, offset: Int) -> Unit, hasOffset: Boolean = true) {
        executeNormalTransform(context, { byteArray, offset ->
            ImageUtils.addEffectToStaticImage(byteArray) { image ->
                effect(image, offset)
            }
        }, hasOffset)
    }


    suspend fun executeNormalTransform(context: CommandContext, transform: (byteArray: ByteArray, offset: Int) -> ByteArrayOutputStream, hasOffset: Boolean = true) {
        val pair = ImageUtils.getImageBytesNMessage(context) ?: return
        val imageByteArray = pair.first ?: return
        val argInt = if (pair.second) 1 else 0
        val offset = (getIntegerFromArgN(context, argInt + 0) ?: 128)

        val outputStream = transform(imageByteArray, offset)
        sendFile(context, outputStream.toByteArray(), "png")
    }


    suspend fun executeGifRecolor(context: CommandContext, recolor: (ints: IntArray) -> IntArray, hasOffset: Boolean = true) {
        executeGifEffect(context, { image, offset ->
            ImageUtils.recolorPixel(image, offset) { ints ->
                recolor(ints)
            }
        }, hasOffset)
    }

    suspend fun executeGifEffect(context: CommandContext, effect: (image: BufferedImage, offset: Int) -> Unit, hasOffset: Boolean = true) {
        executeGifTransform(context, { byteArray, fps, quality, offset ->
            ImageUtils.addEffectToGifFrames(byteArray, fps, quality) { image ->
                effect(image, offset)
            }
        }, hasOffset)
    }


    suspend fun executeGifTransform(context: CommandContext, transform: (byteArray: ByteArray, fps: Float, quality: Int, offset: Int) -> ByteArrayOutputStream, hasOffset: Boolean = true) {
        val pair = ImageUtils.getImageBytesNMessage(context) ?: return
        val imageByteArray = pair.first ?: return
        var argInt = if (pair.second) 1 else 0

        if (!hasOffset) {
            argInt -= 1
        }

        val offset = (getIntegerFromArgN(context, argInt + 0) ?: 128)
        val quality = getIntegerFromArgN(context, argInt + 1) ?: 5
        val fps = (getIntegerFromArgN(context, argInt + 2) ?: 20).toFloat()

        val outputStream = transform(imageByteArray, fps, quality, offset)

        sendFile(context, outputStream.toByteArray(), "gif")
    }


}