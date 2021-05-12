package me.melijn.melijnbot.internals.services.twitter

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import me.melijn.melijnbot.database.socialmedia.TwitterWebhook
import me.melijn.melijnbot.database.socialmedia.TwitterWrapper
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import org.apache.commons.text.StringEscapeUtils
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.collections.set
import kotlin.math.floor

class TwitterService(
    val httpClient: HttpClient,
    private val twitterToken: String,
    private val twitterWrapper: TwitterWrapper,
    val shardManager: ShardManager,
    private val podInfo: PodInfo
) : Service("Twitter", 5, 1, TimeUnit.MINUTES) {

    override val service: RunnableTask = RunnableTask {
        val twitterWebhooks = twitterWrapper.getAll(podInfo)
        val size = twitterWebhooks.size.toDouble()
        val delay = TimeUnit.MILLISECONDS.convert(floor(period / size).toLong(), unit)

        val map = mutableMapOf<Long, Int>()
        for (twitterWebhook in twitterWebhooks) {
            map[twitterWebhook.guildId] = map.getOrDefault(twitterWebhook.guildId, 0) + 1
            if ((map[twitterWebhook.guildId] ?: 0) > 3) continue // current mitigations
            if (twitterWebhook.monthlyTweetCount > 200) continue
            val tweets = fetchNewTweets(twitterWebhook) ?: continue
            postNewTweets(twitterWebhook, tweets)
            updateTwitterWebhookInfo(twitterWebhook, tweets)
            delay(delay)
        }
    }

    private fun updateTwitterWebhookInfo(twitterWebhook: TwitterWebhook, tweets: Tweets) {
        twitterWebhook.lastTweetId = (tweets.newestTweetId ?: return)
        twitterWebhook.monthlyTweetCount += tweets.tweetList.size
        twitterWebhook.lastTweetTime = System.currentTimeMillis()
        twitterWrapper.store(twitterWebhook)
    }

    private suspend fun postNewTweets(twitterWebhook: TwitterWebhook, tweets: Tweets) {
        if (tweets.tweetList.isEmpty()) return
        val body = DataObject.empty()
        val selfUser = shardManager.shards.first().selfUser
        body["username"] = selfUser.name
        body["avatar_url"] = selfUser.effectiveAvatarUrl

        for (tweet in tweets.tweetList.sortedBy { it.createdAt.toEpochSecond(ZoneOffset.UTC) }) {
            if (twitterWebhook.excludedTweetTypes.contains(tweet.type)) continue
            val embed = DataObject.empty()
            val url = "https://twitter.com/${twitterWebhook.handle}/status/${tweet.id}"
            embed["type"] = "rich"
            embed["color"] = 0x1A91DA


            var content = tweet.content
            if (content.takeLastWhile { it != ' ' }.contains("https://t.co/", true)
            ) { // Remove appended tweet urls
                content = content.dropLastWhile { it != ' ' }
            }

            val orderedMentions = tweet.mentions.sortedBy { it.end }.reversed()
            for ((start, end, handle) in orderedMentions) {
                val p1 = content.substring(0, start)
                val p2 = content.substring(end, content.length)
                content = p1 + "[@" + handle + "](https://twitter.com/${handle})" + p2
            }

            content = StringEscapeUtils.unescapeHtml4(content)
            embed["description"] = content
            if (tweet.media.isNotEmpty()) {
                embed["image"] = DataObject.empty().put("url", tweet.media.first().url)
            }

            val title = when (tweet.type) {
                TweetInfo.TweetType.POST -> "${tweets.author.name} posted:"
                TweetInfo.TweetType.REPLY -> "${tweets.author.name} replied:"
                TweetInfo.TweetType.POLL -> "${tweets.author.name} posted a poll:"
                TweetInfo.TweetType.RETWEET -> "${tweets.author.name} retweeted:"
                TweetInfo.TweetType.QUOTED -> "${tweets.author.name} quoted:"
            }

            val author = DataObject.empty()
            author["name"] = title
            author["url"] = url
            author["icon_url"] = tweets.author.avatarUrl
            embed["author"] = author
            embed["timestamp"] = tweet.createdAt.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
            val embeds = DataArray.empty()
            embeds.add(embed)
            body["embeds"] = embeds

            try {
                httpClient.post<HttpResponse>(twitterWebhook.webhookUrl) {
                    this.body = body.toString()
                    header("content-type", "application/json")
                }
            } catch (t: Throwable) {
                t.printStackTrace()
                return
            }
        }
    }

    private val twitterDotCoRegex = "https://t\\.co/[a-zA-Z0-9]+".toRegex()
    private suspend fun fetchNewTweets(twitterWebhook: TwitterWebhook): Tweets? {
        logger.info("Twitter fetch for: ${twitterWebhook.handle} (id=${twitterWebhook.twitterUserId})")
        val patternFormatRFC3339 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
        val lastTweetTime = OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(twitterWebhook.lastTweetTime), ZoneOffset.UTC
        ).format(patternFormatRFC3339)
        val currentTime = OffsetDateTime.ofInstant(
            Instant.now(), ZoneOffset.UTC
        ).format(patternFormatRFC3339)
        val content = DataObject.fromJson(httpClient.get<String>(
            "https://api.twitter.com/2/users/${twitterWebhook.twitterUserId}/tweets"
        ) {
            if (twitterWebhook.lastTweetId != 0L)
                parameter("since_id", twitterWebhook.lastTweetId)
            parameter("max_results", 5)
            parameter(
                "expansions",
                "author_id,entities.mentions.username,attachments.media_keys,referenced_tweets.id"
            )
            when {
                twitterWebhook.excludedTweetTypes.contains(TweetInfo.TweetType.REPLY) && twitterWebhook.excludedTweetTypes.contains(
                    TweetInfo.TweetType.RETWEET
                ) -> {
                    parameter("exclude", "replies,retweets")
                }
                twitterWebhook.excludedTweetTypes.contains(TweetInfo.TweetType.REPLY) -> {
                    parameter("exclude", "replies")
                }
                twitterWebhook.excludedTweetTypes.contains(TweetInfo.TweetType.RETWEET) -> {
                    parameter("exclude", "retweets")
                }
            }
            parameter("tweet.fields", "created_at")
            parameter("media.fields", "preview_image_url,type,url")
            parameter("user.fields", "profile_image_url,username")
            parameter("start_time", lastTweetTime)
            parameter("end_time", currentTime)
            header("Authorization", "Bearer $twitterToken")
        })
        if (!content.hasKey("meta")) {
            return null
        }
        val metaInfo = content.getObject("meta")
        val arrSize = metaInfo.getInt("result_count")

        if (arrSize == 0) {
            return null
        }

        val includes = content.getObjectN("includes")
        val includedUsers = includes?.getArrayN("users") ?: DataArray.empty()
        val includedMedia = includes?.getArrayN("media") ?: DataArray.empty()
        val users = mutableListOf<TwitterUser>()
        val media = mutableListOf<TweetInfo.TwitterMedia>()
        for (i in 0 until includedUsers.length()) {
            val includedUser = includedUsers.getObject(i)
            users.add(
                TwitterUser(
                    includedUser.getString("name"),
                    includedUser.getString("profile_image_url"),
                    includedUser.getString("id").toLong(),
                    includedUser.getString("username"),
                )
            )
        }
        for (i in 0 until includedMedia.length()) {
            val includedMediaObj = includedMedia.getObject(i)
            val mediaUrl = includedMediaObj.getString("url", null)
                ?: includedMediaObj.getString("preview_image_url", null)
                ?: continue

            media.add(
                TweetInfo.TwitterMedia(
                    includedMediaObj.getString("media_key"),
                    includedMediaObj.getString("type"),
                    mediaUrl
                )
            )
        }
        val list = mutableListOf<TweetInfo>()
        val arr = content.getArray("data")
        for (i in 0 until arrSize) {
            val tweetData = arr.getObject(i)
            val mentionedUser = tweetData.getObjectN("entities")?.getArrayN("mentions") ?: DataArray.empty()
            val mentionedMedia = tweetData.getObjectN("attachments")?.getArrayN("media_keys") ?: DataArray.empty()
            val linkedMedia = mutableListOf<TweetInfo.TwitterMedia>()
            val mentions = mutableListOf<MentionedUser>()
            for (n in 0 until mentionedUser.length()) {
                val mention = mentionedUser.getObject(n)
                mentions.add(
                    MentionedUser(
                        mention.getInt("start"),
                        mention.getInt("end"),
                        mention.getString("username"),
                        users.first { it.handle == mention.getString("username") }
                    )
                )
            }
            for (n in 0 until mentionedMedia.length()) {
                val thisMentionedMediaKey = mentionedMedia.getString(n)
                linkedMedia.add(
                    media.first { it.key == thisMentionedMediaKey }
                )
            }

            val createdAt = patternFormatRFC3339.parse(tweetData.getString("created_at").remove(".000"))
            val createdAtMillis = LocalDateTime.from(createdAt)
            val isReply = tweetData.hasKey("in_reply_to_user_id")
            val isReference = tweetData.hasKey("referenced_tweets")

            val text = tweetData.getString("text")
            val type = when {
                isReply -> TweetInfo.TweetType.REPLY
                isReference -> {
                    val referencedTweets = tweetData.getArray("referenced_tweets")

                    @Suppress("MoveVariableDeclarationIntoWhen") // Fuck off intellij this will be a variable
                    val referenceType = referencedTweets.getObject(0).getString("type")
                    when (referenceType) {
                        "retweeted" -> TweetInfo.TweetType.RETWEET
                        "quoted" -> TweetInfo.TweetType.QUOTED
                        "replied_to" -> TweetInfo.TweetType.REPLY
                        else -> {
                            logger.warn("$referenceType is an unimplemented twitter reference type")
                            return null
                        }
                    }
                }
                else -> TweetInfo.TweetType.POST
            }

            val contentType = if (text.matches(twitterDotCoRegex)) {
                TweetInfo.TweetContentType.MEDIA
            } else {
                if (media.isNotEmpty()) TweetInfo.TweetContentType.TEXT_MEDIA
                else TweetInfo.TweetContentType.TEXT
            }

            list.add(
                TweetInfo(
                    tweetData.getLong("id"),
                    twitterWebhook.twitterUserId,
                    text,
                    linkedMedia,
                    mentions,
                    type,
                    contentType,
                    createdAtMillis
                )
            )
        }

        val newestTweetId = metaInfo.getString("newest_id", null)?.toLongOrNull() ?: list.firstOrNull()?.id
        val author = users.first { it.id == twitterWebhook.twitterUserId }
        return Tweets(author, newestTweetId, list)
    }
}

data class Tweets(
    val author: TwitterUser,
    val newestTweetId: Long?,
    val tweetList: List<TweetInfo>
)

data class TwitterUser(
    val name: String,
    val avatarUrl: String,
    val id: Long,
    val handle: String
)

data class MentionedUser(
    val start: Int,
    val end: Int,
    val handle: String,
    val user: TwitterUser
)

data class TweetInfo(
    val id: Long,
    val user_id: Long,
    val content: String,
    val media: List<TwitterMedia>,
    val mentions: List<MentionedUser>,
    val type: TweetType,
    val contentType: TweetContentType,
    val createdAt: LocalDateTime
) {
    data class TwitterMedia(
        val key: String,
        val type: String,
        val url: String
    )

    /** Dont insert, only append entries **/
    enum class TweetType(
        val id: Int,
        val contentTypes: Set<TweetContentType> = setOf(
            TweetContentType.TEXT,
            TweetContentType.MEDIA,
            TweetContentType.TEXT_MEDIA
        ),
        val enabled: Boolean = true
    ) {
        POST(0),
        REPLY(1),
        POLL(2),
        RETWEET(3),
        QUOTED(4);

        companion object {
            fun from(id: Int): TweetType? {
                var type: TweetType? = null
                for (value in values()) {
                    if (value.id == id)
                        type = value
                }
                return type
            }
        }
    }

    /** Dont insert, only append entries **/
    enum class TweetContentType(val id: Int, val enabled: Boolean = true) {
        TEXT(0),
        MEDIA(1),
        TEXT_MEDIA(2);

        companion object {
            fun from(id: Int): TweetContentType? {
                var type: TweetContentType? = null
                for (value in values()) {
                    if (value.id == id)
                        type = value
                }
                return type
            }
        }
    }
}