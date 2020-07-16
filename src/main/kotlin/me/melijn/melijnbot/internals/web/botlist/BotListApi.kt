package me.melijn.melijnbot.internals.web.botlist

import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import kotlinx.coroutines.TimeoutCancellationException
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.*
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BotListApi(val httpClient: HttpClient, val taskManager: TaskManager, val settings: Settings) {

    val logger: Logger = LoggerFactory.getLogger(BotListApi::class.java)

    fun updateTopDotGG(serversArray: List<Long>) {
        val token = settings.tokens.topDotGG
        val url = "$TOP_GG_URL/api/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("shards", DataArray.fromCollection(serversArray))
                .toString()

            postBotStats(url) {
                header("Authorization", token)
                this.body = TextContent(body, ContentType.Application.Json)
            }
        }
    }

    private suspend fun postBotStats(url: String, builder: HttpRequestBuilder.() -> Unit) {
        try {
            httpClient.post<String>(url, builder)
        } catch (t: TimeoutCancellationException) {
            logger.warn("Failed to post bot stats to: $url")
        } catch (t: ClientRequestException) {
            logger.warn("Failed to post bot stats to: $url")
        } catch (t: ServerResponseException) {
            logger.warn("Failed to post bot stats to: $url")
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

            postBotStats(url) {
                header("Authorization", token)
                this.body = TextContent(body, ContentType.Application.Json)
            }
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

            postBotStats(url) {
                header("Authorization", token)
                this.body = TextContent(body, ContentType.Application.Json)
            }
        }
    }

    fun updateDiscordBotListCom(servers: Long, voice: Long) {
        val token = settings.tokens.discordBotListCom
        val url = "$DISCORD_BOT_LIST_COM/api/v1/bots/${settings.id}/stats"
        if (token.isBlank()) return
        taskManager.async {
            val body = DataObject.empty()
                .put("guilds", servers)
                .put("voice_connections", voice)
                .toString()

            postBotStats(url) {
                header("Authorization", token)
                this.body = TextContent(body, ContentType.Application.Json)
            }
            // Might break due to missing content-type header
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

            postBotStats(url) {
                header("Authorization", token)
                this.body = TextContent(body, ContentType.Application.Json)
            }
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

            postBotStats(url) {
                header("Authorization", token)
                this.body = TextContent(body, ContentType.Application.Json)
            }
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

            postBotStats(url) {
                header("Authorization", token)
                this.body = TextContent(body, ContentType.Application.Json)
            }
        }
    }
}