package me.melijn.melijnbot.objects.web.weebsh

import me.duncte123.weebJava.WeebApiBuilder
import me.duncte123.weebJava.models.WeebApi
import me.duncte123.weebJava.types.NSFWMode
import me.duncte123.weebJava.types.TokenType
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.toLCC
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WeebshApi(val settings: Settings) {

    private val weebApi: WeebApi = WeebApiBuilder(TokenType.WOLKETOKENS)
        .setBotInfo(settings.name, settings.version, settings.environment.toLCC())
        .setToken(settings.tokens.weebSh)
        .build()

    suspend fun getUrl(type: String, nsfw: Boolean = false): String = suspendCoroutine {
        weebApi.getRandomImage(type, if (nsfw) NSFWMode.ONLY_NSFW else NSFWMode.DISALLOW_NSFW).async({ image ->
            it.resume(image.url)
        }, { _ ->
            it.resume(MISSING_IMAGE_URL)
        })
    }
}