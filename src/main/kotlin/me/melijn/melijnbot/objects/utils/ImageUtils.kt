package me.melijn.melijnbot.objects.utils

import com.madgag.gif.fmsware.AnimatedGifEncoder
import com.madgag.gif.fmsware.GifDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue
import kotlin.math.sqrt


object ImageUtils {

    suspend fun getBufferedImageNMessage(context: CommandContext): BufferedImage? = suspendCoroutine {
        context.taskManager.executorService.launch {
            val args = context.args
            val attachments = context.getMessage().attachments

            var img: BufferedImage? = null
            if (args.isNotEmpty() && args[0].isNotEmpty()) {
                val user = retrieveUserByArgsN(context, 0)
                if (user != null) {
                    withContext(Dispatchers.IO) {
                        img = ImageIO.read(URL(user.effectiveAvatarUrl + "?size=2048"))
                    }
                } else {
                    try {
                        withContext(Dispatchers.IO) {
                            img = ImageIO.read(URL(args[0]))
                        }
                    } catch (e: Exception) {
                        val msg = i18n.getTranslation(context, "message.wrong.url")
                            .replace(PLACEHOLDER_ARG, args[0])
                        sendMsg(context, msg)
                    }
                }
            } else if (attachments.isNotEmpty()) {
                try {
                    withContext(Dispatchers.IO) {
                        img = ImageIO.read(URL(attachments[0].url + "?size=2048"))
                    }
                } catch (e: Exception) {
                    val msg = i18n.getTranslation(context, "message.attachmentnotanimage")
                        .replace(PLACEHOLDER_ARG, attachments[0].url)
                    sendMsg(context, msg)
                }
            } else {
                withContext(Dispatchers.IO) {
                    img = ImageIO.read(URL(context.getAuthor().effectiveAvatarUrl + "?size=2048"))
                }
            }
            it.resume(img)
        }
    }

    //ByteArray (imageData)
    //Boolean (if it is from an argument -> true) (attachment or noArgs(author)) -> false)
    suspend fun getImageBytesNMessage(context: CommandContext): Pair<ByteArray?, Boolean>? = suspendCoroutine {
        context.taskManager.executorService.launch {
            val args = context.args
            val attachments = context.getMessage().attachments

            var arg = false
            var img: ByteArray? = null
            if (args.isNotEmpty() && args[0].isNotEmpty()) {
                val user = retrieveUserByArgsN(context, 0)
                if (user != null) {
                    arg = true
                    withContext(Dispatchers.IO) {
                        img = URL(user.effectiveAvatarUrl + "?size=2048").readBytes()
                    }
                } else {
                    arg = true
                    try {
                        withContext(Dispatchers.IO) {
                            img = URL(args[0]).readBytes()
                        }
                    } catch (e: Exception) {
                        val msg = i18n.getTranslation(context, "message.wrong.url")
                            .replace(PLACEHOLDER_ARG, args[0])
                        sendMsg(context, msg)
                    }
                }
            } else if (attachments.isNotEmpty()) {
                try {
                    arg = false
                    withContext(Dispatchers.IO) {
                        img = URL(attachments[0].url + "?size=2048").readBytes()
                    }
                } catch (e: Exception) {
                    val msg = i18n.getTranslation(context, "message.attachmentnotanimage")
                        .replace(PLACEHOLDER_ARG, attachments[0].url)
                    sendMsg(context, msg)
                }
            } else {
                arg = false
                withContext(Dispatchers.IO) {
                    img = URL(context.getAuthor().effectiveAvatarUrl + "?size=2048").readBytes()
                }
            }
            val pair = Pair(img, arg)
            it.resume(if (pair.first == null) null else pair)
        }
    }

    private fun getBrightness(r: Int, g: Int, b: Int): Int {
        return sqrt(r * r * .241 + g * g * .691 + b * b * .068).toInt()
    }

    fun getBlurpleForPixel(r: Int, g: Int, b: Int, offset: Int = 128): IntArray {
        val brightness = getBrightness(r, g, b)
        val whiteThreshold = 24 + offset.absoluteValue
        val blurpleThreshold = -43 + offset.absoluteValue
        val invertOffset = offset < 0
        return when {
            brightness >= whiteThreshold -> if (invertOffset) intArrayOf(78, 93, 148) else intArrayOf(255, 255, 255) //wit
            brightness >= blurpleThreshold -> intArrayOf(114, 137, 218) //blurple
            else -> if (invertOffset) intArrayOf(255, 255, 255) else intArrayOf(78, 93, 148) //dark blurple
        }
    }

    fun getSpookyForPixel(r: Int, g: Int, b: Int, offset: Int): IntArray {
        val brightness = getBrightness(r, g, b)

        return if (offset >= 0) {
            if (brightness >= offset) intArrayOf(255, 128, 0) //ORANGE #FF8000
            else intArrayOf(50, 50, 50) //DARK #323232
        } else {
            val i = offset.absoluteValue
            if (brightness >= i) intArrayOf(50, 50, 50) //DARK #323232
            else intArrayOf(255, 128, 0) //ORANGE #FF8000
        }
    }

    fun addEffectToGifFrames(imageByteArray: ByteArray, fps: Float? = null, quality: Int, effect: (BufferedImage) -> Unit): ByteArrayOutputStream {
        val byteArrayInputStream = ByteArrayInputStream(imageByteArray)
        val decoder = GifDecoder()
        val encoder = AnimatedGifEncoder()
        val outputStream = ByteArrayOutputStream()

        encoder.setQuality(quality)
        encoder.start(outputStream)
        encoder.setRepeat(0)
        decoder.read(byteArrayInputStream)


        for (index in 0 until decoder.frameCount) {
            val gifFrame = decoder.getFrame(index)
            effect(gifFrame)
            encoder.setDelay(decoder.getDelay(0))
            if (fps != null) encoder.setFrameRate(fps)
            encoder.addFrame(gifFrame)
        }

        encoder.finish()
        return outputStream
    }

    fun recolorPixel(image: BufferedImage, offset: Int = 128, colorPicker: (IntArray) -> IntArray) {
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                var color = image.getRGB(x, y)

                val a: Int = color shr 24 and 0xff
                val r: Int = color shr 16 and 0xff
                val g: Int = color shr 8 and 0xff
                val b: Int = color and 0xff

                val arr = IntArray(4)
                arr[0] = r
                arr[1] = g
                arr[2] = b
                arr[3] = offset

                val newColor: IntArray = colorPicker(arr)
                color = a shl 24 or (newColor[0] shl 16) or (newColor[1] shl 8) or newColor[2]

                image.setRGB(x, y, color)
            }
        }
    }

    fun addEffectToStaticImage(imageByteArray: ByteArray, effect: (BufferedImage) -> Unit): ByteArrayOutputStream {
        val byteArrayInputStream = ByteArrayInputStream(imageByteArray)
        val image = ImageIO.read(byteArrayInputStream)
        val outputStream = ByteArrayOutputStream()

        effect(image)
        ImageIO.write(image, "png", outputStream)
        return outputStream
    }

    fun getInvertedPixel(r: Int, g: Int, b: Int): IntArray {
        return intArrayOf(255-r, 255-g, 255-b)
    }
}