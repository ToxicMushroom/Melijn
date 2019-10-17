package me.melijn.melijnbot.objects.web


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.threading.TaskManager
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class WebManager(val taskManager: TaskManager) {
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


}