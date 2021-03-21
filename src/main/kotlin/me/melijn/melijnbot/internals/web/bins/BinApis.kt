package me.melijn.melijnbot.internals.web.bins

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.content.*
import io.ktor.http.*
import net.dv8tion.jda.api.utils.data.DataObject

class BinApis(val httpClient: HttpClient) {

    private val host = "ghostbin.co/paste"

    suspend fun postToHastebin(lang: String, content: String): String? {
        return try {
            val result = httpClient.post<String>("https://$host/new") {
                body = TextContent(content, ContentType.Text.Html)
            }

            val json = DataObject.fromJson(result)
            "https://$host/" + json.getString("key") + ".$lang"
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }

    }
}