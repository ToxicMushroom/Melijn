package me.melijn.melijnbot.objects.utils

import com.madgag.gif.fmsware.AnimatedGifEncoder
import com.madgag.gif.fmsware.GifDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.Raster
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue
import kotlin.math.sqrt


object ImageUtils {

    //ByteArray (imageData)
    //Boolean (if it is from an argument -> true) (attachment or noArgs(author)) -> false)
    suspend fun getImageBytesNMessage(context: CommandContext): Triple<ByteArray, String, Boolean>? = suspendCoroutine {
        context.taskManager.executorService.launch {
            val args = context.args
            val attachments = context.message.attachments

            val arg: Boolean
            var img: ByteArray? = null
            val url: String
            if (attachments.isNotEmpty()) {
                try {
                    arg = false
                    url = attachments[0].url + "?size=2048"
                    withContext(Dispatchers.IO) {
                        img = URL(url).readBytes()
                        if (ImageIO.read(ByteArrayInputStream(img)) == null) img = null
                    }
                } catch (e: Exception) {
                    val msg = context.getTranslation("message.attachmentnotanimage")
                            .replace(PLACEHOLDER_ARG, attachments[0].url)
                    sendMsg(context, msg)
                    it.resume(null)
                    return@launch
                }
            } else if (args.isNotEmpty() && args[0].isNotEmpty()) {
                val user = retrieveUserByArgsN(context, 0)
                if (user != null) {
                    arg = true
                    url = user.effectiveAvatarUrl + "?size=2048"
                    withContext(Dispatchers.IO) {
                        img = URL(url).readBytes()
                        if (ImageIO.read(ByteArrayInputStream(img)) == null) img = null
                    }
                } else {
                    arg = true
                    url = args[0]
                    try {
                        withContext(Dispatchers.IO) {
                            img = URL(url).readBytes()
                            if (ImageIO.read(ByteArrayInputStream(img)) == null) {
                                img = null
                            }
                        }
                    } catch (e: Exception) {
                        val msg = context.getTranslation("message.wrong.url")
                                .replace(PLACEHOLDER_ARG, args[0])
                        sendMsg(context, msg)
                        it.resume(null)
                        return@launch
                    }
                }
            } else {
                arg = false
                url = context.author.effectiveAvatarUrl + "?size=2048"
                withContext(Dispatchers.IO) {
                    img = URL(url).readBytes()
                }
            }

            if (img == null) {
                val msg = context.getTranslation("message.notimage")
                        .replace("%url%", url)
                sendMsg(context, msg)
                it.resume(null)
                return@launch
            }

            val nonnullImage: ByteArray = img ?: return@launch
            val triple = Triple(nonnullImage, url, arg)
            it.resume(triple)
        }
    }

    fun getBrightness(r: Int, g: Int, b: Int): Int {
        return sqrt(r * r * .241 + g * g * .691 + b * b * .068).toInt()
    }

    fun getBlurpleForPixel(r: Int, g: Int, b: Int, offset: Int = 128): IntArray {
        val brightness = getBrightness(r, g, b)
        val whiteThreshold = 24 + offset.absoluteValue
        val blurpleThreshold = -43 + offset.absoluteValue
        val invertOffset = offset < 0
        return when {
            brightness >= whiteThreshold -> if (invertOffset) intArrayOf(78, 93, 148) else intArrayOf(254, 254, 254) //wit
            brightness >= blurpleThreshold -> intArrayOf(114, 137, 218) //blurple
            else -> if (invertOffset) intArrayOf(254, 254, 254) else intArrayOf(78, 93, 148) //dark blurple
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

    fun addEffectToGifFrames(imageByteArray: ByteArray, fps: Float? = null, quality: Int, repeat: Boolean?, effect: (BufferedImage) -> Unit): ByteArrayOutputStream {
        val byteArrayInputStream = ByteArrayInputStream(imageByteArray)
        val decoder = GifDecoder()
        val encoder = AnimatedGifEncoder()
        val outputStream = ByteArrayOutputStream()

        encoder.setQuality(quality)
        encoder.start(outputStream)
        decoder.read(byteArrayInputStream)

        val repeatCount = if (repeat != null && repeat == true) {
            0
        } else if (repeat != null && repeat == false) {
            -1
        } else {
            decoder.loopCount
        }
        encoder.setRepeat(repeatCount)
        encoder.setTransparent(Color.WHITE, true)

        for (index in 0 until decoder.frameCount) {
            val frameMeta = decoder.getFrameMeta(index)
            val gifFrame = frameMeta.image


            effect(gifFrame)
            encoder.setDelay(frameMeta.delay)
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
        var arr = intArrayOf(255 - r, 255 - g, 255 - b)
        if (arr[0] == 255 && arr[1] == 255 && arr[2] == 255) {
            arr = intArrayOf(254, 254, 254)
        }
        return arr
    }

    fun pixelate(imageToPixelate: BufferedImage, pixelSize: Int): BufferedImage? {
        val pixelateImage = BufferedImage(
                imageToPixelate.width,
                imageToPixelate.height,
                imageToPixelate.type)
        var y = 0
        while (y < imageToPixelate.height) {
            var x = 0
            while (x < imageToPixelate.width) {
                val croppedImage = getCroppedImage(imageToPixelate, x, y, pixelSize, pixelSize)
                val dominantColor = getDominantColor(croppedImage)
                var yd = y
                while (yd < y + pixelSize && yd < pixelateImage.height) {
                    var xd = x
                    while (xd < x + pixelSize && xd < pixelateImage.width) {
                        pixelateImage.setRGB(xd, yd, dominantColor.rgb)
                        xd++
                    }
                    yd++
                }
                x += pixelSize
            }
            y += pixelSize
        }
        return pixelateImage
    }

    fun getCroppedImage(image: BufferedImage, startx: Int, starty: Int, width: Int, height: Int): BufferedImage {
        var startx1 = startx
        var starty1 = starty
        var width1 = width
        var height1 = height
        if (startx1 < 0) startx1 = 0
        if (starty1 < 0) starty1 = 0
        if (startx1 > image.width) startx1 = image.width
        if (starty1 > image.height) starty1 = image.height
        if (startx1 + width1 > image.width) width1 = image.width - startx1
        if (starty1 + height1 > image.height) height1 = image.height - starty1
        return image.getSubimage(startx1, starty1, width1, height1)
    }

    fun getDominantColor(image: BufferedImage): Color {
        val colorCounter: MutableMap<Int, Int> = HashMap(100)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val currentRGB = image.getRGB(x, y)
                val count = colorCounter.getOrDefault(currentRGB, 0)
                colorCounter[currentRGB] = count + 1
            }
        }
        return getDominantColor(colorCounter)
    }

    private fun getDominantColor(colorCounter: Map<Int, Int>): Color {
        val dominantRGB = colorCounter.entries.stream()
                .max { entry1: Map.Entry<Int, Int>, entry2: Map.Entry<Int, Int> -> if (entry1.value > entry2.value) 1 else -1 }
                .get()
                .key
        return Color(dominantRGB)
    }

    fun pixelatev2(image: BufferedImage, i: Int) {
        // Get the raster data (array of pixels)
        val src: Raster = image.data

        // Create an identically-sized output raster
        val dest = src.createCompatibleWritableRaster()

        // Loop through every i pixels, in both x and y directions
        var y = 0
        while (y < src.height) {
            var x = 0
            while (x < src.width) {
                // Copy the pixel
                var pixel: DoubleArray? = DoubleArray(4)
                pixel = src.getPixel(x, y, pixel)
                // "Paste" the pixel onto the surrounding i by i neighbors
                // Also make sure that our loop never goes outside the bounds of the image
                var yd = y
                while (yd < y + i && yd < dest.height) {
                    var xd = x
                    while (xd < x + i && xd < dest.width) {
                        dest.setPixel(xd, yd, pixel)
                        xd++
                    }
                    yd++
                }
                x += i
            }
            y += i
        }

        // Save the raster back to the Image
        image.data = dest
    }
}