package me.melijn.melijnbot.internals.web.apis

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class ImageApi(val httpClient: HttpClient, val proxiedHttpClient: HttpClient) {

    companion object {
        suspend fun downloadBytes(client: HttpClient, url: String): ByteArray? {
            return try {
                client.get<HttpResponse>(url).readBytes()
            } catch (t: Throwable) {
                null
            }
        }

        suspend fun download(client: HttpClient, url: String): ByteArrayInputStream {
            return ByteArrayInputStream(client.get<HttpResponse>(url).readBytes())
        }

        suspend fun downloadDiscord(
            client: HttpClient,
            url: String,
            size: DiscordSize = DiscordSize.Original
        ): ByteArrayInputStream {
            return download(client, url + size.getParam())
        }

        suspend fun downloadDiscordBytes(
            client: HttpClient,
            url: String,
            size: DiscordSize = DiscordSize.Original
        ): ByteArray? {
            return downloadBytes(client, url + size.getParam())
        }
    }

    suspend fun downloadBytes(url: String, useProxy: Boolean = false): ByteArray? {
        val client = if (useProxy) proxiedHttpClient else httpClient
        return downloadBytes(client, url)
    }

    suspend fun download(url: String, useProxy: Boolean = false): ByteArrayInputStream {
        val client = if (useProxy) proxiedHttpClient else httpClient
        return download(client, url)
    }

    suspend fun downloadDiscord(
        url: String,
        size: DiscordSize = DiscordSize.Original
    ): ByteArrayInputStream {
        return download(httpClient, url + size.getParam())
    }

    suspend fun downloadDiscordBytes(
        url: String,
        size: DiscordSize = DiscordSize.Original
    ): ByteArray? {
        return downloadBytes(httpClient, url + size.getParam())
    }

    suspend fun downloadDiscordImgNMessage(
        context: ICommandContext,
        url: String,
        size: DiscordSize = DiscordSize.Original,
        allowGif: Boolean = true
    ): BufferedImage? {
        return try {
            var urlString = url + size.getParam()
            if (!allowGif) urlString = urlString.replace(".gif", ".png")
            val img = ImageIO.read(downloadDiscord(urlString, size))
            if (img == null) {
                sendMessageNotAnImage(context, url)
                return null
            }
            img
        } catch (t: Throwable) {
            sendErrorDownloadingUrl(context, url)
            return null
        }
    }

    // Method should not assume that the input url is an image
    suspend fun downloadDiscordBytesNMessage(
        context: ICommandContext,
        url: String,
        size: DiscordSize = DiscordSize.Original,
        allowGif: Boolean = true,
        validateImg: Boolean = false
    ): ByteArray? {
        return try {
            var urlString = url + size.getParam()
            if (!allowGif) urlString = urlString.replace(".gif", ".png")
            val bytes = downloadDiscordBytes(urlString, size)
            val img = ImageIO.read(ByteArrayInputStream(bytes))
            if (img == null) {
                sendMessageNotAnImage(context, url)
                return null
            }
            bytes
        } catch (t: Throwable) {
            sendErrorDownloadingUrl(context, url)
            return null
        }
    }

    private suspend fun sendErrorDownloadingUrl(
        context: ICommandContext,
        url: String
    ) {
        sendRsp(context, "Error while downloading $url")
    }

    private suspend fun sendMessageNotAnImage(
        context: ICommandContext,
        url: String
    ) {
        sendRsp(context, "`$url` is not an image")
    }
}