package me.melijn.melijnbot.commandutil.image

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.getBooleanFromArgN
import me.melijn.melijnbot.objects.utils.getIntegerFromArgN
import me.melijn.melijnbot.objects.utils.sendFile
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream

object ImageCommandUtil {

    val defaultOffset = 128
    val defaultQuality = 5

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
        val offset = if (hasOffset) (getIntegerFromArgN(context, argInt + 0) ?: defaultOffset) else defaultOffset

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
        executeGifTransform(context, { byteArray, fps, quality, repeat, offset ->
            ImageUtils.addEffectToGifFrames(byteArray, fps, quality, repeat) { image ->
                effect(image, offset)
            }
        }, hasOffset)
    }


    suspend fun executeGifTransform(context: CommandContext, transform: (byteArray: ByteArray, fps: Float?, quality: Int, repeat: Boolean?, offset: Int) -> ByteArrayOutputStream, hasOffset: Boolean = true) {
        val pair = ImageUtils.getImageBytesNMessage(context) ?: return
        val imageByteArray = pair.first ?: return
        var argInt = if (pair.second) 1 else 0

        if (!hasOffset) {
            argInt -= 1
        }

        val offset = (getIntegerFromArgN(context, argInt + 0) ?: defaultOffset)
        val quality = getIntegerFromArgN(context, argInt + 1) ?: defaultQuality
        val repeat = getBooleanFromArgN(context, argInt + 2)
        val fps = getIntegerFromArgN(context, argInt + 3)?.toFloat()

        val outputStream = transform(imageByteArray, fps, quality, repeat, offset)
        sendFile(context, outputStream.toByteArray(), "gif")
    }


}