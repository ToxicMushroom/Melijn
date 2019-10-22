package me.melijn.melijnbot.objects.web


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.duncte123.weebJava.WeebApiBuilder
import me.duncte123.weebJava.models.WeebApi
import me.duncte123.weebJava.types.TokenType
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.toLCC
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebManager(val taskManager: TaskManager, val settings: Settings) {

    private val weebApi: WeebApi = WeebApiBuilder(TokenType.WOLKETOKENS)
        .setBotInfo(settings.name, settings.version, settings.environment.toLCC())
        .setToken(settings.tokens.weebSh)
        .build()

    private val httpClient = OkHttpClient()

    suspend fun getJsonFromUrl(url: String, parameters: Map<String, String> = emptyMap()): JsonNode? = suspendCoroutine {
        taskManager.async {
            val mapper = ObjectMapper()
            val fullUrlWithParams = url + parameters.entries.joinToString("&", "?",
                transform = { entry ->
                    entry.key + "=" + entry.value
                }
            )
            val request = Request.Builder()
                .url(fullUrlWithParams)
                .get()
                .build()

            val response = httpClient.newCall(request).await()
            val responseBody = response.body
            if (responseBody == null) {
                it.resume(null)
                return@async
            }
            withContext(Dispatchers.IO) {
                val responseString = responseBody.string()
                it.resume(mapper.readTree(responseString))
            }
        }
    }

    suspend fun getWeebJavaUrl(type: String): String = suspendCoroutine {
        weebApi.getRandomImage(type).async({ image ->
            it.resume(image.url)
        }, { _ ->
            it.resume(MISSING_IMAGE_URL)
        })
    }

    suspend fun getWeebTypes(): String = suspendCoroutine{
        weebApi.types.async({ types ->
            it.resume(types.types.joinToString())
        }, { _ ->
            it.resume("error")
        })
    }
}