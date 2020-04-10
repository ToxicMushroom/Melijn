package me.melijn.melijnbot.objects.utils

import com.madgag.gif.fmsware.GifDecoder
import com.squareup.gifencoder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.awt.image.Kernel
import java.awt.image.Raster
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


object ImageUtils {

    //ByteArray (imageData)
    //Boolean (if it is from an argument -> true) (attachment or noArgs(author)) -> false)
    suspend fun getImageBytesNMessage(context: CommandContext, reqFormat: String? = null): Triple<ByteArray, String, Boolean>? {
        val args = context.args
        val attachments = context.message.attachments

        val arg: Boolean
        var img: ByteArray? = null
        val url: String
        if (attachments.isNotEmpty()) {
            try {
                arg = false
                url = attachments[0].url + "?size=2048"

                if (!checkFormat(context, attachments[0].url, reqFormat)) return null
                withContext(Dispatchers.IO) {
                    img = URL(url).readBytes()
                    if (ImageIO.read(ByteArrayInputStream(img)) == null) img = null
                }
            } catch (e: Throwable) {
                val msg = context.getTranslation("message.attachmentnotanimage")
                    .replace(PLACEHOLDER_ARG, attachments[0].url)
                sendMsg(context, msg)
                return null
            }
        } else if (args.isNotEmpty() && args[0].isNotEmpty()) {
            val user = retrieveUserByArgsN(context, 0)
            if (user != null) {
                arg = true
                url = user.effectiveAvatarUrl + "?size=2048"
                if (!checkFormat(context, user.effectiveAvatarUrl, reqFormat)) return null
                withContext(Dispatchers.IO) {
                    img = URL(url).readBytes()
                    if (ImageIO.read(ByteArrayInputStream(img)) == null) img = null
                }
            } else {
                arg = true
                url = args[0]
                try {
                    if (!checkFormat(context, args[0], reqFormat)) return null
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
                    return null
                }
            }
        } else {
            arg = false
            url = context.author.effectiveAvatarUrl + "?size=2048"
            if (!checkFormat(context, context.author.effectiveAvatarUrl, reqFormat)) return null
            withContext(Dispatchers.IO) {
                img = URL(url).readBytes()
            }
        }

        if (img == null) {
            val msg = context.getTranslation("message.notimage")
                .replace("%url%", url)
            sendMsg(context, msg)
            return null
        }

        val nonnullImage: ByteArray = img ?: return null
        return Triple(nonnullImage, url, arg)
    }

    private suspend fun checkFormat(context: CommandContext, url: String, reqFormat: String?): Boolean {
        if (reqFormat != null && !url.contains(reqFormat)) {
            if (context.authorId == 223456683337318402) {
                sendMsg(context, "<:cough:676177448290746379>")
                return false
            }

            val msg = context.getTranslation("message.notagif")
                .replace("%url%", url)
            sendMsg(context, msg)
            return false
        }
        return true
    }

    fun getBrightness(r: Int, g: Int, b: Int): Int {
        return sqrt(r * r * .241 + g * g * .691 + b * b * .068).toInt()
    }

    fun getBlurpleForPixel(r: Int, g: Int, b: Int, a: Int, offset: Int = 128, isGif: Boolean = false): IntArray {
        val brightness = getBrightness(r, g, b)
        val whiteThreshold = 24 + offset.absoluteValue
        val blurpleThreshold = -43 + offset.absoluteValue
        val invertOffset = offset < 0


        if (isGif && a < 128) {
            return intArrayOf(255, 255, 255, 255)
        }

        return when {
            brightness >= whiteThreshold -> if (invertOffset) intArrayOf(78, 93, 148, a) else intArrayOf(254, 254, 254, a) //wit
            brightness >= blurpleThreshold -> intArrayOf(114, 137, 218, a) //blurple
            else -> if (invertOffset) intArrayOf(254, 254, 254, a) else intArrayOf(78, 93, 148, a) //dark blurple
        }
    }

    fun getSpookyForPixel(r: Int, g: Int, b: Int, a: Int, offset: Int, isGif: Boolean = false): IntArray {
        val brightness = getBrightness(r, g, b)

        if (isGif && a < 128) {
            return intArrayOf(255, 255, 255, 255)
        }

        return if (offset >= 0) {
            if (brightness >= offset) intArrayOf(255, 128, 0, a) //ORANGE #FF8000
            else intArrayOf(50, 50, 50, a) //DARK #323232
        } else {
            val i = offset.absoluteValue
            if (brightness >= i) intArrayOf(50, 50, 50, a) //DARK #323232
            else intArrayOf(255, 128, 0, a) //ORANGE #FF8000
        }
    }

    fun addEffectToGifFrames(
        decoder: GifDecoder,
        fps: Float? = null,
        repeat: Boolean?,
        effect: (BufferedImage) -> Unit,
        frameDebug: CommandContext? = null
    ): ByteArrayOutputStream {
        val outputStream = ByteArrayOutputStream()
        val repeatCount = if (repeat != null && repeat == true) {
            0
        } else if (repeat != null && repeat == false) {
            -1
        } else {
            decoder.loopCount
        }

        val width = decoder.getFrame(0).width
        val height = decoder.getFrame(0).height
        val encoder = GifEncoder(outputStream, width, height, repeatCount)


        val gct = decoder.gct ?: emptyArray<Int>().toIntArray()
        for (index in 0 until decoder.frameCount) {
            val options = ImageOptions()
            options.setColorQuantizer(MedianCutQuantizer.INSTANCE)
            options.setDitherer(FloydSteinbergDitherer.INSTANCE)
            options.setTransparencyColor(Color.WHITE.rgb)
            options.setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE)


            val frameMeta = decoder.getFrameMeta(index)
            val gifFrame = frameMeta.image

            effect(gifFrame)

            val delay = fps?.let { (1.0 / it * 1000.0).toLong() } ?: frameMeta.delay.toLong()
            options.setDelay(delay, TimeUnit.MILLISECONDS)

            frameDebug?.let {
                runBlocking {
                    val lct = if (frameMeta.lct.isEmpty()) {
                        gct
                    } else {
                        frameMeta.lct
                    }

                    val bgColor = if (lct.size > frameMeta.bgIndex && frameMeta.bgIndex != -1) {
                        Color(lct[frameMeta.bgIndex])
                    } else {
                        null
                    }

                    val transColor = if (lct.size > frameMeta.transIndex && frameMeta.transIndex != -1) {
                        Color(lct[frameMeta.transIndex])
                    } else {
                        null
                    }

                    sendMsg(it, "bg: $bgColor, trans: $transColor", gifFrame, "gif")
                }
            }

            encoder.addImage(
                gifFrame.getRGB(0, 0, width, height, Array(width * height) { 0 }.toIntArray(), 0, width),
                width, options)
        }

        encoder.finishEncoding()

        return outputStream
    }


    // RGBA <-> color picker
    fun recolorPixelSingleOffset(image: BufferedImage, offset: Int = 128, colorPicker: (IntArray) -> IntArray) {
        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                var color = image.getRGB(x, y)

                val a: Int = color shr 24 and 0xff
                val r: Int = color shr 16 and 0xff
                val g: Int = color shr 8 and 0xff
                val b: Int = color and 0xff

                val arr = IntArray(5)
                arr[0] = r
                arr[1] = g
                arr[2] = b
                arr[3] = a
                arr[4] = offset

                val newColor: IntArray = colorPicker(arr)
                color = newColor[3] shl 24 or (newColor[0] shl 16) or (newColor[1] shl 8) or newColor[2]

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

    fun getInvertedPixel(r: Int, g: Int, b: Int, a: Int, isGif: Boolean = false): IntArray {
        val ir = 255 - r
        val ig = 255 - g
        val ib = 255 - b
        return if (isGif && a < 128) {
            intArrayOf(255, 255, 255, 255)
        } else if (isGif && ir == 255 && ig == 255 && ib == 255) {
            intArrayOf(254, 254, 254, 255)
        } else {
            intArrayOf(ir, ig, ib, a)
        }
    }

    fun smoothPixelate(image: BufferedImage, pixelSize: Int, isGif: Boolean = false) {
        // Get the raster data (array of pixels)
        val src: Raster = image.data

        // Create an identically-sized output raster
        val dest = src.createCompatibleWritableRaster()

        for (x in 0 until image.width step pixelSize) {
            for (y in 0 until image.height step pixelSize) {

                val croppedImage = getCroppedImage(image, x, y, pixelSize, pixelSize)
                val newColor = getDominantColor(croppedImage)
                val cArr = suiteColorArrForGif(newColor.rgb)

                for (xd in x until min(x + pixelSize, image.width)) {
                    for (yd in y until min(y + pixelSize, image.height)) {
                        dest.setPixel(xd, yd, cArr)
                    }
                }
            }
        }

        image.data = dest
    }

    private fun suiteColorArrForGif(bgra: Int): IntArray = when {
        (bgra shr 24 and 0xff) < 128 -> { // Checks if alpha is almost invisible
            intArrayOf(255, 255, 255, 255) // Sets to transparent gif color
        }
        bgra and 0x00ffffff == 16777215 -> { //Cuts off the alpha of the int and compares it with white
            intArrayOf(254, 254, 254, 255)
        }
        else -> {
            intArrayOf(
//                rgba shr 24 and 0xff,
//                rgba shr 16 and 0xff,
//                rgba shr 8 and 0xff,
//                rgba and 0xff
                bgra shr 16 and 0xff,
                bgra shr 8 and 0xff,
                bgra and 0xff,
                bgra shr 24 and 0xff
            )
        }
    }

    private fun getCroppedImage(image: BufferedImage, startx: Int, starty: Int, width: Int, height: Int): BufferedImage {
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

    private fun getDominantColor(image: BufferedImage): Color {
        val colorCounter: MutableMap<Int, Int> = HashMap()
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
        return Color(dominantRGB, true)
    }

    fun pixelate(image: BufferedImage, size: Int, isGif: Boolean = false) {
        // Get the raster data (array of pixels)
        val src: Raster = image.data

        // Create an identically-sized output raster
        val dest = src.createCompatibleWritableRaster()
        val rFloatArr = intArrayOf(0, 0, 0, 0)
        // Loop through every i pixels, in both x and y directions
        for (y in 0 until src.height step size) {
            for (x in 0 until src.width step size) {
                // Copy the pixel
                val pixel = src.getPixel(x, y, rFloatArr)

                val cArr = if (isGif && pixel[3] < 128)
                    intArrayOf(255, 255, 255, 255)
                else if (isGif && pixel[0] == 255 && pixel[1] == 255 && pixel[2] == 255)
                    intArrayOf(254, 254, 254, 255)
                else pixel


                // "Paste" the pixel onto the surrounding i by i neighbors
                // Also make sure that our loop never goes outside the bounds of the image
                for (xd in x until min(x + size, image.width)) {
                    for (yd in y until min(y + size, image.height)) {
                        dest.setPixel(xd, yd, cArr)
                    }
                }
            }
        }

        // Save the raster back to the  Image
        image.data = dest
    }

    fun blur(image: BufferedImage, radius: Int, isGif: Boolean = false) {
        val size = radius * 2 + 1
        val weight = 1.0f / (size * size)
        val data = FloatArray(size * size)


        for (index in data.indices) {
            data[index] = weight
        }

        val kernel = Kernel(size, size, data)
        useKernel(image, kernel, isGif)
    }


    fun sharpen(image: BufferedImage, i: Int, isGif: Boolean = false) {
        val sharpenForce = i.toFloat()
        val data = FloatArray(3 * 3)
        data[1] = -1 * sharpenForce
        data[4] = data[1]
        data[6] = data[1]
        data[8] = data[1]
        data[5] = 4 * sharpenForce + 1
        val kernel = Kernel(3, 3, data)

        useKernel(image, kernel, isGif)
    }

    private fun useKernel(image: BufferedImage, kernel: Kernel, shouldMakeGifAlphaSupport: Boolean = false) {
        // Get the raster data (array of pixels)
        val src: Raster = image.data

        // Create an identically-sized output raster
        val dest = src.createCompatibleWritableRaster()

        val rFloatArr = floatArrayOf(0f, 0f, 0f, 0f)
        val kData = kernel.getKernelData(null)
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                var newRed = 0f
                var newGreen = 0f
                var newBlue = 0f
                var newAlpha = 0f


                for (yk in 0 until kernel.height) {
                    for (xk in 0 until kernel.width) {
                        val kry = kernel.yOrigin - yk
                        val krx = kernel.xOrigin - xk
                        val pixel = src.getPixel(
                            if (x + krx >= image.width || x + krx < 0) {
                                x
                            } else {
                                x + krx
                            },
                            if (y + kry >= image.height || y + kry < 0) {
                                y
                            } else {
                                y + kry
                            }, rFloatArr)

                        val kernelModifier = kData[yk * kernel.width + xk]
                        newRed += kernelModifier * pixel[0]
                        newGreen += kernelModifier * pixel[1]
                        newBlue += kernelModifier * pixel[2]
                        newAlpha += kernelModifier * pixel[3]
                    }
                }

                newAlpha = max(0f, min(255f, newAlpha))
                val intArr = if (shouldMakeGifAlphaSupport && newAlpha < 128) {
                    floatArrayOf(255f, 255f, 255f, 255f)
                } else if (shouldMakeGifAlphaSupport && newRed >= 255f && newGreen >= 255f && newBlue >= 255f) {
                    floatArrayOf(254f, 254f, 254f, 255f)
                } else {
                    floatArrayOf(
                        max(0f, min(255f, newRed)),
                        max(0f, min(255f, newGreen)),
                        max(0f, min(255f, newBlue)),
                        newAlpha
                    )
                }

                dest.setPixel(x, y,
                    intArr
                )
            }
        }
        image.data = dest
    }

    fun putText(bufferedImage: BufferedImage, text: String, startX: Int, endX: Int, startY: Int, graphics: Graphics): BufferedImage {
        val fontMetrics = graphics.getFontMetrics(graphics.font)
        val lineWidth = endX - startX
        val lineHeight = fontMetrics.height
        if (fontMetrics.stringWidth(text) <= lineWidth) {
            graphics.drawString(text, startX, startY + lineHeight)
        } else {
            val sb = StringBuilder()
            val parts = text.split("\\s+".toRegex()).toTypedArray()
            for (part in parts) {
                val currentLineContent = sb.substring(max(0, sb.lastIndexOf("\n")), sb.length)
                val possibleFutureLineContent = if (currentLineContent.isEmpty()) part else "$currentLineContent $part"
                when {
                    fontMetrics.stringWidth(part) > lineWidth -> {
                        var contentWidth = fontMetrics.stringWidth("$currentLineContent ")
                        val dashWidth = fontMetrics.charWidth('-')
                        sb.append(" ")
                        for ((partProgress, c) in part.toCharArray().withIndex()) {
                            val charWidth = fontMetrics.charWidth(c)
                            if (contentWidth + dashWidth + charWidth > lineWidth) {
                                if (partProgress != 0) sb.append("-\n") else sb.append("\n")
                                contentWidth = 0
                            }
                            sb.append(c)
                            contentWidth += fontMetrics.charWidth(c)
                        }
                    }
                    fontMetrics.stringWidth(possibleFutureLineContent) > lineWidth -> {
                        sb.append("\n").append(part)
                    }
                    else -> {
                        if (sb.isNotEmpty()) sb.append(" ")
                        sb.append(part)
                    }
                }
            }
            var i = 0
            for (line in sb.toString().split("\n".toRegex()).toTypedArray()) {
                i++
                graphics.drawString(line, 1133, 82 + lineHeight * i)
            }
        }
        graphics.dispose()
        return bufferedImage
    }

    fun suiteColorForGif(argb: Int): Int = when {
        (argb shr 24 and 0xff) < 128 -> { // Checks if alpha is almost invisible
            -1 // Sets to transparent gif color
        }
        argb and 0x00ffffff == 16777215 -> { //Cuts off the alpha of the int and compares it with white
            ((argb shr 24 and 0xff shl 24) // Only the alpha is visible here
                or (254 shl 16) // integrates other values into the int
                or (254 shl 8)
                or (254 shl 0))
        }
        else -> argb
    }
}