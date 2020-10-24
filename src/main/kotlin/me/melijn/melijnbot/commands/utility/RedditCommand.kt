package me.melijn.melijnbot.commands.utility

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.lettuce.core.SetArgs
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import me.melijn.melijnbot.objectMapper
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import kotlin.random.Random

class RedditCommand : AbstractCommand("command.reddit") {

    init {
        id = 202
        name = "reddit"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }


        val subreddit = getStringFromArgsNMessage(context, 0, 1, 1000, mustMatch = Regex("(?:r/)?[a-zA-Z0-9_]+"))
            ?.removePrefix("r/") ?: return

        val arg = context.args.getOrNull(1) ?: "hot"
        val time = context.args.getOrNull(2) ?: "day"

        val randomResult = try {
            getRandomRedditResultNMessage(context, subreddit, arg, time) ?: return
        } catch (t: ClientRequestException) {
            val unknownReddit = context.getTranslation("$root.unknown")
                .withVariable("subreddit", subreddit)
            sendRsp(context, unknownReddit)
            return
        }
        val embedder = Embedder(context)
            .setTitle(randomResult.title.take(256), "https://reddit.com" + randomResult.url)
            .setImage(if (randomResult.justText) null else randomResult.img)
            .setThumbnail(if (randomResult.justText) "https://static.melijn.com/text-icon.png" else null)
            .setFooter("\uD83D\uDD3C ${randomResult.ups} | " + (randomResult.created * 1000).asEpochMillisToDateTime(context.getTimeZoneId()))
        if (randomResult.thumb.isNotBlank() && randomResult.thumb != "self" && randomResult.justText) {
            try {
                embedder.setThumbnail(randomResult.thumb)
            } catch (e: Exception) {
            }
        }
        if (randomResult.justText) {
            embedder.setTitle(null, null)
                .setDescription(randomResult.title.take(MessageEmbed.TEXT_MAX_LENGTH - 256) + "\n[link](https://reddit.com${randomResult.url})")
        }

        sendEmbedRsp(context, embedder.build())
        // send fancy embed
    }

    companion object {
        suspend fun getRandomRedditResultNMessage(context: CommandContext, subreddit: String, arg: String, time: String): RedditResult? {
            val about = context.daoManager.driverManager.redisConnection?.async()
                ?.get("reddit:about:$subreddit")
                ?.await()
                ?.let {
                    objectMapper.readValue<RedditAbout>(it)
                } ?: requestAboutAndStore(context.webManager.httpClient, context.daoManager.driverManager, subreddit)

            if (about == null) {
                val unknownReddit = context.getTranslation("command.reddit.down")
                    .withSafeVariable("subreddit", "r/$subreddit")
                sendRsp(context, unknownReddit)
                return null
            }

            if (about.private) {
                val unknownReddit = context.getTranslation("command.reddit.private")
                    .withSafeVariable("subreddit", "r/$subreddit")
                sendRsp(context, unknownReddit)
                return null
            }

            if (!about.exists) {
                val unknownReddit = context.getTranslation("command.reddit.unknown")
                    .withSafeVariable("subreddit", "r/$subreddit")
                sendRsp(context, unknownReddit)
                return null
            }

            if (about.over18 && context.isFromGuild && !context.textChannel.isNSFW) {
                // send stinky nsfw warning
                val msg = context.getTranslation("command.reddit.subnsfw")
                    .withSafeVariable("subreddit", "r/$subreddit")
                sendRsp(context, msg)
                return null
            }

            val timePart = if (arg == "top") {
                ":$time"
            } else ""
            val posts = context.daoManager.driverManager.redisConnection?.async()
                ?.get("reddit:posts:$arg$timePart:$subreddit")
                ?.await()
                ?.let {
                    objectMapper.readValue<List<RedditResult>>(it)
                }
                ?: requestPostsAndStore(context.webManager.httpClient, context.daoManager.driverManager, subreddit, arg, time)

            if (posts == null) {
                val unknownReddit = context.getTranslation("command.reddit.down")
                    .withSafeVariable("subreddit", "r/$subreddit")
                sendRsp(context, unknownReddit)
                return null
            }

            val filteredPosts = posts.filter { !it.nsfw || (it.nsfw && (!context.isFromGuild || context.textChannel.isNSFW)) }
            if (filteredPosts.isEmpty() && posts.isNotEmpty()) {
                val msg = context.getTranslation("command.reddit.allpostsnsfw")
                    .withVariable("amount", posts.size - filteredPosts.size)
                sendRsp(context, msg)
                return null
            } else if (posts.isEmpty()) {
                val msg = context.getTranslation("command.reddit.empty")
                    .withSafeVariable("subreddit", "r/$subreddit")
                sendRsp(context, msg)
                return null
            }
            return filteredPosts[Random.nextInt(filteredPosts.size)]
        }

        suspend fun requestPostsAndStore(httpClient: HttpClient, driverManager: DriverManager, subreddit: String, arg: String, time: String): List<RedditResult>? {
            val data = try {
                DataObject.fromJson(
                    httpClient.get<String>("https://www.reddit.com/r/$subreddit.json?sort=${arg}&t=${time}&limit=100")
                ).getObject("data")
            } catch (t: Throwable) {
                return null
            }

            val posts = mutableListOf<RedditResult>()
            val dataPosts = data.getArray("children")
            for (i in 0 until dataPosts.length()) {
                val dataPost = dataPosts.getObject(i).getObject("data")
                val imgUrl = dataPost.getString("url_overridden_by_dest", "")
                posts.add(RedditResult(
                    dataPost.getLong("ups"),
                    dataPost.getString("subreddit"),
                    dataPost.getString("title"),
                    dataPost.getLong("created"),
                    dataPost.getString("permalink"),
                    dataPost.getString("url_overridden_by_dest", ""),
                    dataPost.getString("thumbnail") == "self" ||
                        (!imgUrl.endsWith(".png") && !imgUrl.endsWith(".jpg") && !imgUrl.endsWith(".jpeg") && !imgUrl.endsWith(".gif")
                            && !imgUrl.endsWith(".tiff")),
                    dataPost.getBoolean("over18"),
                    dataPost.getString("thumbnail")
                ))
            }
            val timePart = if (arg == "top") {
                ":${time}"
            } else ""
            driverManager.redisConnection?.async()
                ?.set("reddit:posts:${arg}$timePart:$subreddit", objectMapper.writeValueAsString(posts), SetArgs().ex(600))
            return posts
        }

        suspend fun requestAboutAndStore(httpClient: HttpClient, driverManager: DriverManager, subreddit: String): RedditAbout? {
            val res = try {
                DataObject.fromJson(
                    httpClient.get<HttpResponse>("https://api.reddit.com/r/$subreddit/about").readText()
                )
            } catch (t: Throwable) {
                return null
            }
            val about = if (res.hasKey("error") && res.getInt("error") == 403 && res.getString("reason") == "private") {
                RedditAbout(exists = true, private = true, over18 = false)
            } else if (res.hasKey("error") && res.getInt("error") == 404) {
                RedditAbout(exists = false, false, false)
            } else {
                val data = res.getObject("data")
                RedditAbout(data.hasKey("over18"), false, data.getBoolean("over18", false))
            }
            driverManager.redisConnection?.async()
                ?.set("reddit:about:$subreddit", objectMapper.writeValueAsString(about), SetArgs().ex(1800))
            return about
        }
    }

    data class RedditResult(
        val ups: Long,
        val subreddit: String,
        val title: String,
        val created: Long,
        val url: String,
        val img: String,
        val justText: Boolean,
        val nsfw: Boolean,
        val thumb: String
    )

    data class RedditAbout(
        val exists: Boolean,
        val private: Boolean,
        val over18: Boolean
    )
}