package me.melijn.melijnbot.objects.web.bins

import me.melijn.melijnbot.objects.web.WebUtils
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.gildor.coroutines.okhttp.await

class BinApis(val httpClient: OkHttpClient) {

    suspend fun postToHastebin(lang: String, content: String): String? {
        val request = Request.Builder()
            .url("https://hasteb.in/documents")
            .post(content.toByteArray().toRequestBody(WebUtils.textMedia))
            .build()
        val req = httpClient.newCall(request).await()
        val body = req.body ?: return null
        val json = DataObject.fromJson(body.string())
        return "https://hasteb.in/" + json.getString("key") + ".$lang"
    }
}