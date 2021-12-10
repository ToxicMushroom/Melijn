package me.melijn.melijnbot.internals.web.apis

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.future.await
import me.duncte123.weebJava.WeebApiBuilder
import me.duncte123.weebJava.configs.ImageConfig
import me.duncte123.weebJava.models.WeebApi
import me.duncte123.weebJava.types.NSFWMode
import me.duncte123.weebJava.types.TokenType
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.toLCC
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WeebApi(val httpClient: HttpClient, val settings: Settings) {

    private val logger: Logger = LoggerFactory.getLogger("WeebApi")
    private val weebshApi: WeebApi = WeebApiBuilder(TokenType.WOLKETOKENS)
        .setBotInfo("Melijn", "latest", settings.environment.toLCC())
        .setToken(settings.tokens.weebSh)
        .build()

    suspend fun getUrlWaterfall(type: String, nsfw: Boolean = false, apiOrder: Array<Type> = emptyArray()): String {
        for (api in apiOrder + Type.values().filterNot { apiOrder.contains(it) }) {
            val url = when (api) {
                Type.WEEBSH -> getWeebshUrl(type, nsfw)
                Type.SHIRO, Type.SHIRO_NSFW -> getShiroUrl(type, nsfw)
                Type.MIKI -> getMikiUrl(type, nsfw)
            }
            if (url != null) return url
        }

        return MISSING_IMAGE_URL
    }

    var rotate = 0
    suspend fun getUrlRandom(type: String, nsfw: Boolean = false): String {
        val shiroTypes = if (nsfw) arrayOf(Type.SHIRO, Type.SHIRO_NSFW) else arrayOf(Type.SHIRO)
        val url = when (rotate % 3) {
            0 -> getUrlWaterfall(type, nsfw, shiroTypes)
            1 -> getUrlWaterfall(type, nsfw, arrayOf(Type.WEEBSH))
            2 -> getUrlWaterfall(type, nsfw, arrayOf(Type.MIKI))
            else -> throw IllegalStateException("shouldn't reach this")
        }

        if (rotate == 2) rotate = 0
        else rotate++

        return url
    }

    private suspend fun getMikiUrl(type: String, nsfw: Boolean): String? {
        if (nsfw || !mikiList.contains(type)) return null

        val url = "https://api.miki.bot/images/random?tags=$type"
        val mikiResponse = httpClient.getOrNull<MikiResponse>(url, logger = logger)
        return mikiResponse?.url
    }

    private suspend fun getShiroUrl(type: String, nsfw: Boolean): String? {
        if (nsfw && !nsfwShiroList.contains(type)) return null
        if (!nsfw && !shiroList.contains(type)) return null

        val endpoint = when (nsfw) {
            true -> "images/nsfw"
            false -> "images"
        }

        val url = "https://api.dbot.dev/$endpoint/$type"
        val shiroResponse = httpClient.getOrNull<ShiroResponse>(url, logger = logger)
        return shiroResponse?.url
    }

    private suspend fun getWeebshUrl(type: String, nsfw: Boolean): String? {
        val nsfwMode = when (nsfw) {
            true -> NSFWMode.ONLY_NSFW
            false -> NSFWMode.DISALLOW_NSFW
        }
        return try {
            val imgConfig = ImageConfig.Builder()
                .setType(type)
                .setNsfwMode(nsfwMode)
                .build()
            weebshApi.getRandomImage(imgConfig).submit().await()?.url
        } catch (t: Throwable) {
            null
        }
    }

    companion object {
        private val mikiList = listOf(
            "confused",
            "lewd",
            "pout",
            "stare",
            "cry",
            "smug",
            "highfive",
            "hug",
            "kiss",
            "lick",
            "pet",
            "poke",
            "slap",
            "shrug",
            "hug",
            "grope",
            "adore"
        )

        private val shiroList = listOf(
            "avatars",
            "blush",
            "cry",
            "hug",
            "kiss",
            "lick",
            "neko",
            "nom",
            "pat",
            "poke",
            "pout",
            "punch",
            "slap",
            "smug",
            "sleep",
            "tickle",
            "trap",
            "wallpapers"
        )

        private val nsfwShiroList = listOf(
            "bondage",
            "hentai",
            "thighs"
        )
    }

    enum class Type {
        WEEBSH,
        SHIRO,
        SHIRO_NSFW,
        MIKI
    }

    data class ShiroResponse(
        val code: Int,
        val url: String
    )

    data class MikiResponse(
        val id: Long,
        val tags: List<String>,
        val url: String
    )
}

suspend inline fun <reified T> HttpClient.getOrNull(
    urlString: String,
    block: HttpRequestBuilder.() -> Unit = {},
    logger: Logger? = null,
): T? {
    return try {
        get {
            url.takeFrom(urlString)
            block()
        }
    } catch (t: Throwable) {
        logger?.error("Error getting result from: $urlString", t)
        null
    }
}
