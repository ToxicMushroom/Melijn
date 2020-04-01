package me.melijn.melijnbot.commandutil.image

import com.madgag.gif.fmsware.GifDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.getBooleanFromArgN
import me.melijn.melijnbot.objects.utils.getIntegerFromArgN
import me.melijn.melijnbot.objects.utils.sendFile
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

object ImageCommandUtil {

    private const val DEFAULT_OFFSET = 128
    private const val DEFAULT_QUALITY = 5

    suspend fun executeNormalRecolor(
        context: CommandContext,
        recolor: (ints: IntArray) -> IntArray,
        hasOffset: Boolean = true,
        defaultOffset: (img: BufferedImage) -> Int = { DEFAULT_OFFSET },
        offsetRange: (img: BufferedImage) -> IntRange = { IntRange(-255, 255) }
    ) {
        executeNormalEffect(context, { image, offset ->
            ImageUtils.recolorPixel(image, offset) { ints ->
                recolor(ints)
            }
        }, hasOffset, defaultOffset, offsetRange)
    }

    suspend fun executeNormalEffect(
        context: CommandContext,
        effect: (image: BufferedImage, offset: Int) -> Unit,
        hasOffset: Boolean = true,
        defaultOffset: (img: BufferedImage) -> Int = { DEFAULT_OFFSET },
        offsetRange: (img: BufferedImage) -> IntRange = { IntRange(-255, 255) }
    ) {
        executeNormalTransform(context, { byteArray, offset ->
            ImageUtils.addEffectToStaticImage(byteArray) { image ->
                effect(image, offset)
            }
        }, hasOffset, defaultOffset, offsetRange)
    }


    private suspend fun executeNormalTransform(
        context: CommandContext,
        transform: (byteArray: ByteArray, offset: Int) -> ByteArrayOutputStream,
        hasOffset: Boolean = true,
        defaultOffset: (img: BufferedImage) -> Int = { DEFAULT_OFFSET },
        offsetRange: (img: BufferedImage) -> IntRange = { IntRange(-255, 255) }
    ) {
        val triple = ImageUtils.getImageBytesNMessage(context) ?: return
        val imageByteArray = triple.first
        val argInt = if (triple.third) 1 else 0

        val img = withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(triple.first))
        }

        val range = offsetRange(img)
        val offset = if (hasOffset) {
            (getIntegerFromArgN(context, argInt + 0, range.first, range.last) ?: defaultOffset(img))
        } else defaultOffset(img)

        val outputStream = transform(imageByteArray, offset)
        sendFile(context, outputStream.toByteArray(), "png")
    }


    suspend fun executeGifRecolor(
        context: CommandContext,
        recolor: (ints: IntArray) -> IntArray, // ints: r, g, b, offset (def: 128)
        hasOffset: Boolean = true,
        defaultOffset: (img: BufferedImage) -> Int = { DEFAULT_OFFSET },
        offsetRange: (img: BufferedImage) -> IntRange = { IntRange(-255, 255) },
        debug: Boolean = false
    ) {
        executeGifEffect(context, { image, offset ->
            ImageUtils.recolorPixel(image, offset) { ints ->
                recolor(ints)
            }
        }, hasOffset, defaultOffset, offsetRange, debug)
    }

    suspend fun executeGifEffect(
        context: CommandContext,
        effect: (image: BufferedImage, offset: Int) -> Unit,
        hasOffset: Boolean = true,
        defaultOffset: (img: BufferedImage) -> Int = { DEFAULT_OFFSET },
        offsetRange: (img: BufferedImage) -> IntRange = { IntRange(-255, 255) },
        debug: Boolean = false
    ) {
        executeGifTransform(context, { gifDecoder, fps, quality, repeat, offset ->
            if (debug) ImageUtils.addEffectToGifFrames(gifDecoder, fps, quality, repeat, { image ->
                effect(image, offset)
            }, context)
            else ImageUtils.addEffectToGifFrames(gifDecoder, fps, quality, repeat, { image ->
                effect(image, offset)
            })

        }, hasOffset, defaultOffset, offsetRange)
    }


    private suspend fun executeGifTransform(
        context: CommandContext,
        transform: (decoder: GifDecoder, fps: Float?, quality: Int, repeat: Boolean?, offset: Int) -> ByteArrayOutputStream,
        hasOffset: Boolean = true,
        defaultOffset: (img: BufferedImage) -> Int = { DEFAULT_OFFSET },
        offsetRange: (img: BufferedImage) -> IntRange = { IntRange(-255, 255) }
    ) {
        val triple = ImageUtils.getImageBytesNMessage(context, "gif") ?: return
        var argInt = if (triple.third) 1 else 0

        if (!hasOffset) {
            argInt -= 1
        }

        //╯︿╰

        val decoder = GifDecoder()
        val inputStream = ByteArrayInputStream(triple.first)
        decoder.read(inputStream)
        val img = decoder.image
        val range = offsetRange(img)

        val offset = if (hasOffset) {
            (getIntegerFromArgN(context, argInt + 0, range.first, range.last) ?: defaultOffset(img))
        } else {
            defaultOffset(img)
        }

        val quality = getIntegerFromArgN(context, argInt + 1) ?: DEFAULT_QUALITY
        val repeat = getBooleanFromArgN(context, argInt + 2)
        val fps = getIntegerFromArgN(context, argInt + 3)?.toFloat()

        val outputStream = transform(decoder, fps, quality, repeat, offset)
        sendFile(context, outputStream.toByteArray(), "gif")
    }
}