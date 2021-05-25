package me.melijn.melijnbot.internals.utils

import com.sksamuel.scrimage.pixels.Pixel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.*
import io.ktor.utils.io.streams.*
import me.melijn.melijnbot.commands.image.StinkyException
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sqrt

open class TypedByteArray(
    open val bytes: ByteArray,
    open val type: ImageType
)

open class ParsedImageByteArray(
    override val bytes: ByteArray,
    open val url: String,
    override val type: ImageType,
    open val usedArgument: Boolean
) : TypedByteArray(bytes, type)

data class NamedParsedImageByteArray(
    override val bytes: ByteArray,
    override val type: ImageType,
    val name: String
) : TypedByteArray(bytes, type)

data class ParsedImages(
    val images: List<NamedParsedImageByteArray>,
    val usedArgument: Boolean
)

enum class ImageType {
    PNG, JPG, GIF, TIFF
}


object ImageUtils {

    fun createPlane(side: Int, color: Int): BufferedImage {
        val bufferedImage = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
        val graphics2D = bufferedImage.createGraphics()
        graphics2D.color = Color(color)
        graphics2D.fillRect(0, 0, bufferedImage.width, bufferedImage.height)
        return bufferedImage
    }


    suspend fun getImageBytesNMessage(
        context: ICommandContext,
        index: Int,
        discordSize: DiscordSize = DiscordSize.X1024,
        acceptTypes: Set<ImageType>? = null
    ): ParsedImageByteArray? {
        if (acceptTypes?.isEmpty() == true) throw StinkyException()

        val args = context.args
        val attachments = context.message.attachments
        if (attachments.isNotEmpty()) {
            val attachmentUrl = attachments[0].url
            val url = attachmentUrl + discordSize.getParam()
            val type = getFormatNMessage(context, attachmentUrl, acceptTypes) ?: return null
            val img = downloadBytesNMessage(context, url) ?: return null

            return ParsedImageByteArray(img, url, type, false)

        } else if (args.size > index) {
            val arg = args[index]
            val user = retrieveUserByArgsN(context, index)
            if (user != null) {
                val url = changeUrlToFitTypes(user.effectiveAvatarUrl, acceptTypes) + discordSize.getParam()
                val type = getFormatNMessage(context, url, acceptTypes) ?: return null
                val img = downloadBytesNMessage(context, url) ?: return null

                return ParsedImageByteArray(img, url, type, true)

            } else if (EMOTE_MENTION.matches(arg)) {
                val emoteType = if (arg.startsWith("<a")) ImageType.GIF else ImageType.PNG
                val emoteId = EMOTE_MENTION.find(arg)?.groupValues?.get(2)
                val url = changeUrlToFitTypes(
                    "https://cdn.discordapp.com/emojis/$emoteId.${emoteType.toString().lowercase()}",
                    acceptTypes
                ) + discordSize.getParam()
                val type = getFormatNMessage(context, url, acceptTypes) ?: return null
                val img = downloadBytesNMessage(context, url, false) ?: return null

                return ParsedImageByteArray(img, url, type, true)

            } else {
                if (!isValidUrlMessage(context, arg)) return null
                val url = changeUrlToFitTypes(arg, acceptTypes)
                val type = getFormatNMessage(context, url, acceptTypes) ?: return null
                val img = downloadBytesNMessage(context, url, proxy = true) ?: return null

                return ParsedImageByteArray(img, url, type, true)
            }
        }

        val url = changeUrlToFitTypes(context.author.effectiveAvatarUrl, acceptTypes) + discordSize.getParam()
        val type = getFormatNMessage(context, url, acceptTypes) ?: return null
        val img = downloadBytesNMessage(context, url) ?: return null

        return ParsedImageByteArray(img, url, type, true)
    }

    private val typePattern = "\\.(?:${ImageType.values().joinToString("|") { it.toString() }})"
        .toRegex(RegexOption.IGNORE_CASE)

    private fun changeUrlToFitTypes(url: String, acceptTypes: Set<ImageType>?): String {
        if (acceptTypes == null || acceptTypes.isEmpty()) return url
        if (acceptTypes.contains(ImageType.GIF) && url.endsWith(".gif", true)) return url
        if (acceptTypes.any { url.endsWith(".$it", true) }) return url

        val importance = { type: ImageType ->
            when (type) {
                ImageType.PNG -> 1
                ImageType.GIF -> 2
                ImageType.JPG -> 3
                ImageType.TIFF -> 4
            }
        }

        val bestType = acceptTypes.minByOrNull(importance) ?: throw StinkyException()
        return url.replace(typePattern, ".${bestType.toString().lowercase()}")
    }

    suspend fun downloadBytesNMessage(
        context: ICommandContext,
        url: String,
        doChecks: Boolean = true,
        proxy: Boolean = false
    ): ByteArray? {
        val bytes = downloadBytes(context, url, doChecks, proxy)
        if (bytes == null || bytes.isEmpty()) {
            sendRsp(context, "Couldn't download your image :/\nUrl: %url%"
                .withVariable("url", url)
            )
            return null
        }
        return bytes
    }

    suspend fun downloadBytes(
        context: ICommandContext,
        url: String,
        doChecks: Boolean = true,
        proxy: Boolean = false
    ): ByteArray? {
        val client = if (proxy) context.webManager.proxiedHttpClient else context.webManager.httpClient
        return downloadBytes(client, url, false, context.guildN, context)
    }

    suspend fun downloadBytes(
        httpClient: HttpClient,
        url: String,
        doChecks: Boolean = true, // TODO: figure out why doChecks is cringe
        guild: Guild? = null,
        context: ICommandContext? = null
    ): ByteArray? {
        val barr: ByteArray?
        httpClient.get<HttpResponse>(url).let {
            if (!doChecks) {
                barr = it.readBytes()
                return@let
            }
            val channel = it.receive<ByteReadChannel>()
            val baos = ByteArrayOutputStream()

            val maxBytes = guild?.maxFileSize ?: Message.MAX_FILE_SIZE.toLong()
            while (channel.availableForRead != 0) {
                val read = channel.readRemaining(4096)
                baos.writePacket(read)

                if (baos.size() > maxBytes) {
                    context?.let { ctx ->
                        val msg = ctx.getTranslation("message.filetobig")
                            .withVariable("size", "${maxBytes / 1_048_576}MB")
                        sendRsp(ctx, msg)
                    }

                    channel.cancel()
                    barr = null
                    return@let
                }
            }
            barr = baos.toByteArray()
        }
        return barr
    }

    //ByteArray (imageData)
    //String (urls)
    //Boolean (if it is from an argument -> true) (attachment or noArgs(author)) -> false)
    suspend fun getImagesBytesNMessage(
        context: ICommandContext,
        index: Int
    ): ParsedImages? {
        val args = context.args
        val attachments = context.message.attachments

        if (attachments.isNotEmpty()) {
            val namedImgList = mutableListOf<NamedParsedImageByteArray>()
            for (attachment in attachments) {
                var url = attachment.url
                val isZip = url.endsWith(".zip")

                if (isZip) {
                    val barr = downloadBytesNMessage(context, url, doChecks = true, proxy = true) ?: return null
                    extractByteArraysFromZip(barr, namedImgList)
                } else {
                    url = changeUrlToFitTypes(url, setOf(ImageType.PNG))
                    val bytes = downloadBytesNMessage(context, url, doChecks = true, proxy = true) ?: return null
                    namedImgList.add(
                        NamedParsedImageByteArray(
                            bytes,
                            ImageType.PNG,
                            attachment.fileName.take(16)
                        )
                    )
                }
            }
        } else if (args.size > index) {
            val arg = args[index]
            val urls = arg.replace("\n", " ").split(SPACE_PATTERN)

            val namedImgList = mutableListOf<NamedParsedImageByteArray>()
            for ((i, url1) in urls.withIndex()) {
                val isZip = url1.endsWith(".zip")
                if (isZip) {
                    val zipBytes = downloadBytesNMessage(context, url1, doChecks = true, proxy = true) ?: return null
                    extractByteArraysFromZip(zipBytes, namedImgList)


                } else {
                    val bytes = downloadBytesNMessage(context, url1, doChecks = true, proxy = true) ?: return null
                    namedImgList.add(
                        NamedParsedImageByteArray(
                            bytes,
                            ImageType.PNG,
                            "$i"
                        )
                    )
                }
            }
            return ParsedImages(namedImgList, true)
        }
        sendSyntax(context)
        return null
    }

    private fun extractByteArraysFromZip(
        barr: ByteArray,
        namedImgList: MutableList<NamedParsedImageByteArray>
    ) {
        ZipInputStream(barr.inputStream()).use { zis ->
            var ze = zis.nextEntry

            while (ze != null) {
                val bytes = zis.readAllBytes()
                namedImgList.add(
                    NamedParsedImageByteArray(
                        bytes,
                        ImageType.PNG,
                        ze.name
                    )
                )

                ze = zis.nextEntry
            }
        }
    }

    private suspend fun getFormatNMessage(
        context: ICommandContext,
        url: String,
        reqFormat: Set<ImageType>?
    ): ImageType? {
        val formats = reqFormat ?: ImageType.values().toSet()

        val format = formats.maxByOrNull { url.lastIndexOf(it.toString(), ignoreCase = true) }
        if (format == null) {
            val msg = context.getTranslation("message.notcorrectformat")
                .withSafeVariable("url", url)
                .withSafeVariable("formats", formats.joinToString(", "))
            sendRsp(context, msg)
            return null
        }

        if (!isValidUrlMessage(context, url)) {
            return null
        }
        return format
    }

    private suspend fun isValidUrlMessage(context: ICommandContext, url: String): Boolean {
        if (!url.matches(URL_PATTERN)) {
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

    private val blurpleColorMap: (Int) -> Triple<Color, Color, Color> = { a: Int ->
        Triple(
            Color(78, 93, 148, a), // dark blurple
            Color(114, 137, 218, a), // blurple
            Color(254, 254, 254, a) // white
        )
    }
    private val fakeBlurpleColorMap: (Int) -> Triple<Color, Color, Color> = { a: Int ->
        Triple(
            Color(69, 79, 191, a), // dark blurple
            Color(88, 101, 242, a), // blurple
            Color(254, 254, 254, a) // white
        )
    }
    private val spookyColorMap: (Int) -> Pair<Color, Color> = { a: Int ->
        Color(255, 128, 0, a) to // ORANGE #FF8000
            Color(50, 50, 50, a) // DARK #323232
    }

    private fun getTriColorForPixel(
        r: Int, g: Int, b: Int, a: Int, triColor: (Int) -> Triple<Color, Color, Color>,
        offset: Int = 128, isGif: Boolean = false
    ): Color {
        val brightness = getBrightness(r, g, b)
        val lightThreshold = 24 + offset.absoluteValue
        val normalThreshold = -43 + offset.absoluteValue
        val invertOffset = offset < 0

        if (isGif && a < 128) {
            return Color(255, 255, 255, 0)
        }

        val (dark, normal, light) = triColor(a)
        return when {
            brightness >= lightThreshold -> if (invertOffset) dark else light
            brightness >= normalThreshold -> normal
            else -> if (invertOffset) light else dark
        }
    }

    fun getBlurpleForPixel(pixel: Pixel, offset: Int = 128, isGif: Boolean = false): Color {
        return getTriColorForPixel(
            pixel.red(),
            pixel.green(),
            pixel.blue(),
            pixel.alpha(),
            blurpleColorMap,
            offset,
            isGif
        )
    }

    // Rebranded blurple
    fun getFakeBlurpleForPixel(pixel: Pixel, offset: Int = 128, isGif: Boolean = false): Color {
        return getTriColorForPixel(
            pixel.red(),
            pixel.green(),
            pixel.blue(),
            pixel.alpha(),
            fakeBlurpleColorMap,
            offset,
            isGif
        )
    }

    private fun getDiColorForPixel(
        r: Int, g: Int, b: Int, a: Int, diColor: (Int) -> Pair<Color, Color>,
        offset: Int = 128, isGif: Boolean = false
    ): Color {
        val brightness = getBrightness(r, g, b)
        if (isGif && a < 128) {
            return Color(255, 255, 255, 0)
        }

        val (light, dark) = diColor(a)

        return if (offset >= 0) {
            if (brightness >= offset) light
            else dark
        } else {
            val i = offset.absoluteValue
            if (brightness >= i) dark
            else light
        }
    }

    fun getSpookyForPixel(pixel: Pixel, offset: Int, isGif: Boolean = false): Color {
        return getDiColorForPixel(
            pixel.red(),
            pixel.green(),
            pixel.blue(),
            pixel.alpha(),
            spookyColorMap,
            offset,
            isGif
        )
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
}