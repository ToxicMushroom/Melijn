package me.melijn.melijnbot.objects.web.cancer

import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.web.WebUtils.jsonMedia
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory
import java.io.IOException

class BotListApi(val httpClient: OkHttpClient, val taskManager: TaskManager, val settings: Settings) {
    val logger = LoggerFactory.getLogger(BotListApi::class.java)

    private val defaultCallbackHandler = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            logger.error(e.message ?: return)
        }

        override fun onResponse(call: Call, response: Response) {
            response.close()
        }
    }

    fun updateTopDotGG(serversArray: List<Long>) {
        val token = settings.tokens.topDotGG
        val url = "$TOP_GG_URL/api/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("shards", DataArray.fromCollection(serversArray))
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateBotsOnDiscordXYZ(servers: Long) {
        val token = settings.tokens.botsOnDiscordXYZ
        val url = "$BOTS_ON_DISCORD_XYZ_URL/bot-api/bots/${settings.id}/guilds"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("guildCount", "$servers")
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateBotlistSpace(serversArray: List<Long>) {
        val token = settings.tokens.botlistSpace
        val url = "$BOTLIST_SPACE/v1/bots/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("shards", DataArray.fromCollection(serversArray))
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateDiscordBotListCom(servers: Long, voice: Long) {
        val token = settings.tokens.discordBotListCom
        val url = "$DISCORD_BOT_LIST_COM/api/bots/v1/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("guilds", servers)
                .put("voice_connections", voice)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateDiscordBotsGG(servers: Long, shards: Long) {
        val token = settings.tokens.discordBotsGG
        val url = "$DISCORD_BOTS_GG/api/v1/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("guildCount", servers)
                .put("shardCount", shards)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateBotsForDiscordCom(servers: Long) {
        val token = settings.tokens.botsForDiscordCom
        val url = "$BOTS_FOR_DISCORD_COM/api/bot/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("server_count", servers)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }

    fun updateDiscordBoats(servers: Long) {
        val token = settings.tokens.discordBoats
        val url = "$DISCORD_BOATS/api/bot/${settings.id}"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("server_count", servers)
                .toString()

            val request = Request.Builder()
                .addHeader("Authorization", token)
                .url(url)
                .post(body.toRequestBody(jsonMedia))
                .build()

            httpClient.newCall(request).enqueue(defaultCallbackHandler)
        }
    }
}