package me.melijn.melijnbot.internals.utils

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.*
import me.melijn.gifdecoder.GifDecoder
import me.melijn.gifencoder.*
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.web.apis.DiscordSize
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.awt.image.Kernel
import java.awt.image.Raster
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import javax.naming.SizeLimitExceededException
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


object ImageUtils {

    fun createPlane(side: Int, color: Int): BufferedImage {
        val bufferedImage = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
        val graphics2D = bufferedImage.createGraphics()
        graphics2D.color = Color(color)
        graphics2D.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
        return bufferedImage
    }

    //ByteArray (imageData)
    //Boolean (if it is from an argument -> true) (attachment or noArgs(author)) -> false)
    private const val discordSize = "?size=2048"

    suspend fun getImageBytesNMessage(
        context: ICommandContext,
        reqFormat: String? = null
    ): Triple<ByteArray, String, Boolean>? {
        val args = context.args
        val attachments = context.message.attachments

        val arg: Boolean
        var img: ByteArray?
        val url: String

        if (attachments.isNotEmpty()) {
            try {
                arg = false
                url = attachments[0].url + discordSize

                if (!checkFormat(context, attachments[0].url, reqFormat)) return null

                img = downloadImage(context, url)
                ByteArrayInputStream(img).use { bis ->
                    if (ImageIO.read(bis) == null) img = null
                }

            } catch (t: SizeLimitExceededException) {
                t.printStackTrace()
                return null
            } catch (e: Throwable) {
                val msg = context.getTranslation("message.attachmentnotanimage")
                    .withVariable(PLACEHOLDER_ARG, attachments[0].url)
                sendRsp(context, msg)
                return null
            }
        } else if (args.isNotEmpty() && args[0].isNotEmpty()) {
            val user = retrieveUserByArgsN(context, 0)
            if (user != null) {
                arg = true
                val fixedForm = if (reqFormat == "png") {
                    user.effectiveAvatarUrl.split(".").dropLast(1).joinToString(".") + ".png"
                } else {
                    user.effectiveAvatarUrl
                }
                url = fixedForm + discordSize
                if (!checkFormat(context, fixedForm, reqFormat)) return null

                img = downloadImage(context, url)
                ByteArrayInputStream(img).use { bis ->
                    if (ImageIO.read(bis) == null) img = null
                }

            } else if (EMOTE_MENTION.matches(args[0])) {
                arg = true

                val emoteType = if (args[0].startsWith("<a")) "gif" else "png"
                val emoteId = EMOTE_MENTION.find(args[0])?.groupValues?.get(2)

                url = "https://cdn.discordapp.com/emojis/$emoteId.$emoteType?v=1"
                img = downloadImage(context, url)
            } else {
                arg = true
                url = args[0]
                try {
                    if (reqFormat == "gif" && !checkFormat(context, url, reqFormat)) return null
                    if (!checkValidUrl(context, url)) return null

                    img = downloadImage(context.webManager.proxiedHttpClient, url)
                    ByteArrayInputStream(img).use { bis ->
                        if (ImageIO.read(bis) == null) {
                            img = null
                        }
                    }

                } catch (t: SizeLimitExceededException) {
                    t.printStackTrace()
                    return null
                } catch (e: Exception) {
                    e.printStackTrace()
                    val msg = context.getTranslation("message.notuserorurl")
                        .withSafeVariable(PLACEHOLDER_ARG, args[0])
                    sendRsp(context, msg)
                    return null
                }
            }
        } else {
            arg = false
            val fixedForm = if (reqFormat == "png") {
                context.author.effectiveAvatarUrl.split(".").dropLast(1).joinToString(".") + ".png"
            } else {
                context.author.effectiveAvatarUrl
            }
            url = fixedForm + discordSize
            if (!checkFormat(context, fixedForm, reqFormat)) return null
            img = downloadImage(context, url)
        }

        if (img == null) {
            val msg = context.getTranslation("message.notimage")
                .withSafeVariable("url", url)
            sendRsp(context, msg)
            return null
        }

        val nonnullImage: ByteArray = img ?: return null
        return Triple(nonnullImage, url, arg)
    }

    suspend fun downloadImage(context: ICommandContext, url: String, doChecks: Boolean = true): ByteArray {
        return downloadImage(context.webManager.httpClient, url, doChecks, context.guildN, context)
    }

    suspend fun downloadImage(
        httpClient: HttpClient,
        url: String,
        doChecks: Boolean = true,
        guild: Guild? = null,
        context: ICommandContext? = null
    ): ByteArray {
        return if (doChecks) {
            httpClient.get<HttpStatement>(url).execute {
                val channel = it.receive<ByteReadChannel>()
                var running = true


                ByteArrayOutputStream().use { baos ->
                    var totalBytes = 0L
                    val toCompare = guild?.maxFileSize ?: Message.MAX_FILE_SIZE.toLong()
                    while (running) {
                        val read = channel.readRemaining(4096)
                        val readsize = read.remaining
                        totalBytes += readsize
                        baos.writePacket(read)
                        if (totalBytes > toCompare) {
                            running = false

                            context?.let { ctx ->
                                val msg = ctx.getTranslation("message.filetobig")
                                    .withVariable("size", "100MB")
                                sendRsp(ctx, msg)
                            }

                            channel.cancel()
                            throw SizeLimitExceededException("Size limit $toCompare")
                        }
                        if (channel.availableForRead == 0) {
                            running = false
                        }
                    }
                    baos.toByteArray()
                }
            }
        } else {
            httpClient.get(url)
        }
    }

    //ByteArray (imageData)
    //String (urls)
    //Boolean (if it is from an argument -> true) (attachment or noArgs(author)) -> false)
    suspend fun getImagesBytesNMessage(
        context: ICommandContext,
        reqFormat: String? = null
    ): Triple<Map<String, ByteArray>, Pair<Int, Int>, Boolean>? {
        val args = context.args
        val attachments = context.message.attachments
        val imgSize = DiscordSize.X1024
        val imgApi = context.webManager.imageApi

        //filename, imagedata
        val imgMap: MutableMap<String, ByteArray> = mutableMapOf()
        var error = false
        var errorFile: String? = null
        var url = ""
        val arg: Boolean
        var maxWidth = 0
        var maxHeight = 0

        if (attachments.isNotEmpty()) {
            arg = false
            for (attachment in attachments) {
                try {
                    url = attachment.url
                    val isZip = url.endsWith(".zip")

                    if (isZip) {
                        // Attachment is a zip
                        val zipBytes = imgApi.downloadDiscordBytesNMessage(context, url) ?: return null
                        ZipInputStream(zipBytes.inputStream()).use { zis ->
                            var ze = zis.nextEntry

                            while (ze != null) {
                                val imgBytes = zis.readAllBytes()

                                val img = ImageIO.read(imgBytes.inputStream())
                                if (img == null) {
                                    error = true
                                    errorFile = url + " > " + ze.name
                                } else {
                                    maxWidth = max(maxWidth, img.width)
                                    maxHeight = max(maxHeight, img.height)
                                    imgMap[ze.name] = imgBytes
                                }
                                ze = zis.nextEntry
                            }
                        }
                    } else {
                        // Attachment is not a zip, download and analyze
                        url = attachment.url + imgSize.getParam()
                        if (!checkFormat(context, attachment.url, reqFormat)) return null

                        val img = imgApi.downloadDiscordBytesNMessage(
                            context, url, imgSize, allowGif = true,
                            validateImg = true
                        ) ?: return null

                        imgMap[attachment.fileName] = img
                    }
                } catch (e: Throwable) {
                    val msg = context.getTranslation("message.attachmentnotanimage")
                        .withVariable(PLACEHOLDER_ARG, url)
                    sendRsp(context, msg)
                    return null
                }
            }
        } else if (args.isNotEmpty() && args[0].isNotEmpty()) {
            arg = true
            val urls = args[0].replace("\n", " ").split(SPACE_PATTERN)

            try {
                for ((index, url1) in urls.withIndex()) {
                    val isZip = url1.endsWith(".zip")

                    if (isZip) {
                        // One of the arguments is a zip file
                        val zipBytes = imgApi.downloadDiscordBytesNMessage(context, url) ?: return null
                        ZipInputStream(zipBytes.inputStream()).use { zis ->
                            var ze = zis.nextEntry

                            while (ze != null) {
                                val tempZe = ze ?: continue
                                val imgBytes = zis.readAllBytes()

                                val anImage = ImageIO.read(imgBytes.inputStream())
                                if (anImage == null) {
                                    sendRsp(context, "Not an image")
                                    return null
                                } else {
                                    maxWidth = max(maxWidth, anImage.width)
                                    maxHeight = max(maxHeight, anImage.height)
                                    imgMap[tempZe.name] = imgBytes
                                }
                                ze = zis.nextEntry
                            }
                        }
                    } else {
                        // One of the arguments is an image

                        if (!checkFormat(context, url1, reqFormat)) return null

                        val img = withContext(Dispatchers.IO) {
                            context.webManager.proxiedHttpClient.get<HttpResponse>(url1).readBytes()
                        }

                        ByteArrayInputStream(img).use { bais ->
                            if (ImageIO.read(bais) == null) {
                                error = true
                                errorFile = url1
                            }
                        }
                        if (error) break

                        imgMap["$index"] = img
                    }
                }
            } catch (e: Throwable) {
                val msg = context.getTranslation("message.attachmentnotanimage")
                    .withSafeVariable(PLACEHOLDER_ARG, args[0])
                sendRsp(context, msg)
                return null
            }
        } else {
            sendSyntax(context)
            return null
        }

        if (error && imgMap.isEmpty()) {
            val msg = context.getTranslation("message.notimage")
                .withSafeVariable("url", errorFile ?: "")
            sendRsp(context, msg)
            return null
        }

        return Triple(imgMap, Pair(maxWidth, maxHeight), arg)
    }

    private suspend fun checkFormat(context: ICommandContext, url: String, reqFormat: String?): Boolean {
        if (reqFormat != null && !url.contains(reqFormat)) {
            val msg = context.getTranslation("message.notagif")
                .withSafeVariable("url", url)
            sendRsp(context, msg)
            return false
        }

        return checkValidUrl(context, url)
    }

    private suspend fun checkValidUrl(context: ICommandContext, url: String): Boolean {
        if (!URL_PATTERN.matches(url)) {
            val msg = context.getTranslation("message.notaurl")
                .withSafeVariable("url", url)
            sendRsp(context, msg)
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
            brightness >= whiteThreshold -> if (invertOffset) intArrayOf(78, 93, 148, a) else intArrayOf(
                254,
                254,
                254,
                a
            ) //wit
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
            else intArrayOf(255, 128, 0, a) //ORANGE #FF8000jd
        }
    }

    suspend fun addEffectToGifFrames(
        decoder: GifDecoder,
        fps: Float? = null,
        repeat: Boolean?,
        effect: (BufferedImage) -> Unit,
        frameDebug: ICommandContext? = null
    ): ByteArrayOutputStream {
        val outputStream = ByteArrayOutputStream()
        val repeatCount = ImageCommandUtil.fetchRepeatInt(repeat, decoder)

        val width = decoder.getFrame(0).width
        val height = decoder.getFrame(0).height
        val encoder = GifEncoder(outputStream, width, height, repeatCount)


        val gct = decoder.gct ?: emptyArray<Int>().toIntArray()
        val jobs = mutableListOf<Job>()
        val framesDone = mutableMapOf<Int, FinishedFrame>()

        for (index in 0 until decoder.frameCount) {
            jobs.add(CoroutineScope(Dispatchers.Default).launch {
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
                    ImageCommandUtil.sendDebugGifFrame(frameMeta, gct, it, gifFrame)
                }

                framesDone[index] = FinishedFrame(
                    gifFrame.getRGB(0, 0, width, height, IntArray(width * height), 0, width),
                    width,
                    options
                )
            })
        }

        jobs.joinAll()

        for (element in framesDone.entries.sortedBy { it.key }) {
            val frame = element.value
            encoder.addImage(frame.rgbArr, frame.imageWidth, frame.options)
        }

        encoder.finishEncoding()

        return outputStream
    }


    data class FinishedFrame(val rgbArr: IntArray, val imageWidth: Int, val options: ImageOptions)


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
        val image = ByteArrayInputStream(imageByteArray).use { bais ->
            ImageIO.read(bais)
        }

        effect(image)

        val outputStream = ByteArrayOutputStream()
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

    fun smoothPixelate(image: BufferedImage, pixelSize: Int) {
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

    private fun getCroppedImage(
        image: BufferedImage,
        startx: Int,
        starty: Int,
        width: Int,
        height: Int
    ): BufferedImage {
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
        val data = FloatArray(size * size) { weight }
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
        val floatBuffer = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f)
        val kData = kernel.getKernelData(null)
        val kHeight = kernel.height
        val kWidth = kernel.width

        val kYOrigin = kernel.yOrigin
        val kXOrigin = kernel.xOrigin

        val iWidth = image.width
        val iHeight = image.height

        require(kWidth % 2 == 1 && kHeight % 2 == 1) { "kernel should have odd number of pixels" }

        val fArr = FloatArray(iHeight * 4 * kWidth)
        var pixels: FloatArray = src.getPixels(0, 0, kWidth, iHeight, fArr)
        var currentCenter = kotlin.math.floor(kWidth / 2.0)
        val kSideWidth = (kWidth - 1) / 2


        for (x in 0 until iWidth) {
            val mostLeft = x - kSideWidth
            if (x > currentCenter && x < iWidth - kSideWidth) {
                pixels = src.getPixels(x - kSideWidth, 0, kWidth, iHeight, fArr)
                currentCenter = x.toDouble()
            }
            for (y in 0 until iHeight) {
                var newRed = 0f
                var newGreen = 0f
                var newBlue = 0f
                var newAlpha = 0f


                for (yk in 0 until kHeight) {
                    for (xk in 0 until kWidth) {
                        val kry = kYOrigin - yk
                        val krx = kXOrigin - xk

                        val absoluteX = if (x + krx >= iWidth || x + krx < 0) {
                            x
                        } else {
                            x + krx
                        }

                        val absoluteY = if (y + kry >= iHeight || y + kry < 0) {
                            y
                        } else {
                            y + kry
                        }

                        val startIndex = (absoluteX - mostLeft) * 4 + absoluteY * kWidth * 4

                        val kernelModifier = kData[yk * kWidth + xk]
                        newRed += kernelModifier * pixels[startIndex]
                        newGreen += kernelModifier * pixels[startIndex + 1]
                        newBlue += kernelModifier * pixels[startIndex + 2]

                        if (kry == 0 && krx == 0)
                            newAlpha += pixels[startIndex + 3]
                    }
                }

                if (shouldMakeGifAlphaSupport && newAlpha < 128) {
                    floatBuffer[0] = 255f
                    floatBuffer[1] = 255f
                    floatBuffer[2] = 255f
                    floatBuffer[3] = 255f

                } else if (shouldMakeGifAlphaSupport && newRed >= 255f && newGreen >= 255f && newBlue >= 255f) {
                    floatBuffer[0] = 254f
                    floatBuffer[1] = 254f
                    floatBuffer[2] = 254f
                    floatBuffer[3] = 255f

                } else {
                    floatBuffer[0] = newRed
                    floatBuffer[1] = newGreen
                    floatBuffer[2] = newBlue
                    floatBuffer[3] = newAlpha

                }

                dest.setPixel(
                    x, y,
                    floatBuffer
                )
            }
        }
        image.data = dest
    }

    fun putText(
        bufferedImage: BufferedImage,
        text: String,
        startX: Int,
        endX: Int,
        startY: Int,
        graphics: Graphics
    ): BufferedImage {
        val fontMetrics = graphics.getFontMetrics(graphics.font)
        val lineWidth = endX - startX
        val lineHeight = fontMetrics.height
        if (fontMetrics.stringWidth(text) <= lineWidth) {
            graphics.drawString(text, startX, startY + lineHeight)
        } else {
            val sb = StringBuilder()
            val parts = text.split(SPACE_PATTERN).toTypedArray()
            for (part in parts) {
                val currentLineContent = sb.substring(max(0, sb.lastIndexOf('\n')), sb.length)
                val possibleFutureLineContent = if (currentLineContent.isEmpty()) part else "$currentLineContent $part"
                when {
                    fontMetrics.stringWidth(part) > lineWidth -> {
                        var contentWidth = fontMetrics.stringWidth("$currentLineContent ")
                        val dashWidth = fontMetrics.charWidth('-')
                        sb.append(' ')
                        for ((partProgress, c) in part.toCharArray().withIndex()) {
                            val charWidth = fontMetrics.charWidth(c)
                            if (contentWidth + dashWidth + charWidth > lineWidth) {
                                if (partProgress != 0) sb.append("-\n") else sb.appendLine()
                                contentWidth = 0
                            }
                            sb.append(c)
                            contentWidth += fontMetrics.charWidth(c)
                        }
                    }
                    fontMetrics.stringWidth(possibleFutureLineContent) > lineWidth -> {
                        sb.appendLine().append(part)
                    }
                    else -> {
                        if (sb.isNotEmpty()) sb.append(' ')
                        sb.append(part)
                    }
                }
            }
            var i = 0
            for (line in sb.toString().split('\n').toTypedArray()) {
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
        argb and 0x00ffffff == 16777215 -> { // Cuts off the alpha of the int and compares it with white
            ((argb shr 24 and 0xff shl 24) // Only the alpha is visible here
                or (254 shl 16) // integrates other values into the int
                or (254 shl 8)
                or (254 shl 0))
        }
        else -> argb
    }
}