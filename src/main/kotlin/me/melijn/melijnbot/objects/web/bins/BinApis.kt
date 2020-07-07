package me.melijn.melijnbot.objects.web.bins

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import net.dv8tion.jda.api.utils.data.DataObject

class BinApis(val httpClient: HttpClient) {

    suspend fun postToHastebin(lang: String, content: String): String? {
        val result = httpClient.post<String>("https://hasteb.in/documents") {
            body = TextContent(content, ContentType.Text.Html)
        }

        val json = DataObject.fromJson(result)
        return "https://hasteb.in/" + json.getString("key") + ".$lang"
    }
}