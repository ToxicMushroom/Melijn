package me.melijn.melijnbot.internals.services.twitter

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import me.melijn.melijnbot.database.socialmedia.TwitterWebhook
import me.melijn.melijnbot.database.socialmedia.TwitterWrapper
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import net.dv8tion.jda.api.utils.data.DataObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.floor

class TwitterService(
    val httpClient: HttpClient,
    val twitterToken: String,
    val twitterWrapper: TwitterWrapper
) : Service("Twitter", 5, 1, TimeUnit.MINUTES) {

    override val service: RunnableTask = RunnableTask {
        val twitterWebhooks = twitterWrapper.getAll()
        val size = twitterWebhooks.size.toDouble()
        val delay = TimeUnit.MILLISECONDS.convert(floor(period / size).toLong(), TimeUnit.MINUTES)
        println("twitter fetches running with a ${delay}ms delay")

        for (twitterWebhook in twitterWebhooks) {
            val tweets = fetchNewTweets(twitterWebhook)
            postNewTweets(twitterWebhook, tweets)
            updateTwitterWebhookInfo(twitterWebhook, tweets)
            delay(delay)
        }
    }

    private fun updateTwitterWebhookInfo(twitterWebhook: TwitterWebhook, tweets: List<TweetInfo>) {
        TODO("Not yet implemented")
    }

    private fun postNewTweets(twitterWebhook: TwitterWebhook, tweets: List<TweetInfo>) {
        TODO("Not yet implemented")
    }

    private suspend fun fetchNewTweets(twitterWebhook: TwitterWebhook): List<TweetInfo> {
        val lastTweetTime = OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(twitterWebhook.lastTweetTime), ZoneOffset.UTC
        )
        val content = DataObject.fromJson(httpClient.get<String>(
            "https://api.twitter.com/2/users/3092919093/tweets"
        ) {
            parameter("until_id", twitterWebhook.lastTweetId)
            parameter("max_results", 5)
            parameter(
                "start_time",
                lastTweetTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
            )
            header("Authorization", "Bearer $twitterToken")
        })
        println(content)
        val arrSize = content.getObject("meta").getInt("result_count")
        print("Fetched: $arrSize tweets")

        if (arrSize == 0) {
            return emptyList()
        }

        val list = mutableListOf<TweetInfo>()
        val arr = content.getArray("data")
        for (i in 0 until arrSize) {
            val tweetData = arr.getObject(i)


            list.add(
                TweetInfo(

                )
            )
        }
        return list
    }
}

data class TweetInfo(
    val id: Long,
    val user_id: Long,
    val content: String,
    val media: List<TwitterMedia>,
    val type: TwitterWebhook.TweetType
) {
    data class TwitterMedia(
        val url: String
    )
}