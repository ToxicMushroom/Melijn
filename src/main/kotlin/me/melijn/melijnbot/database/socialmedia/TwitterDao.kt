package me.melijn.melijnbot.database.socialmedia

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import java.sql.ResultSet
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TwitterDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "twitter_webhooks"
    override val tableStructure: String = "guild_id bigint, webhook_url varchar(256), excluded_tweet_types int[]," +
        " handle varchar(16), twitter_user_id bigint, monthly_tweet_count bigint, last_tweet_id bigint," +
        " last_tweet_time bigint, month_start bigint, enabled boolean"
    override val primaryKey: String = "guild_id, handle"

    override val cacheName: String = "twitter"

    fun store(twitterWebhook: TwitterWebhook) = twitterWebhook.apply {
        driverManager.executeUpdate(
            "INSERT INTO $table (guild_id, webhook_url, excluded_tweet_types," +
                " handle, twitter_user_id, monthly_tweet_count, last_tweet_id, " +
                " last_tweet_time, month_start, enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT ($primaryKey)" +
                " DO UPDATE SET webhook_url = ?, excluded_tweet_types = ?, twitter_user_id = ?, monthly_tweet_count = ?," +
                " last_tweet_id = ?, last_tweet_time = ?, month_start = ?, enabled = ?",
            guildId, webhookUrl, excludedTweetTypes, handle, twitterUserId, monthlyTweetCount, lastTweetId,
            lastTweetTime, monthStart, enabled,

            // UPDATE SET:
            webhookUrl, excludedTweetTypes, twitterUserId, monthlyTweetCount, lastTweetId, lastTweetTime, monthStart,
            enabled
        )
    }

    suspend fun getAll(): List<TwitterWebhook> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHERE enabled = ?", handleWebhookResults(it),
            true
        )
    }

    suspend fun getAll(guildId: Long): List<TwitterWebhook> = suspendCoroutine {
        driverManager.executeQuery(
            "SELECT * FROM $table WHRE guild_id = ? AND enabled = ?", handleWebhookResults(it),
            guildId, true
        )
    }

    private fun handleWebhookResults(it: Continuation<List<TwitterWebhook>>) = { rs: ResultSet ->
        val webhooks = mutableListOf<TwitterWebhook>()

        while (rs.next()) {
            webhooks.add(
                TwitterWebhook(
                    rs.getLong("guild_id"),
                    rs.getString("webhook_url"),
                    rs.getObject("excluded_tweet_types", Set::class.java).mapNotNull {
                        TwitterWebhook.TweetType.from(it.toString().toInt())
                    }.toSet(),
                    rs.getString("handle"),
                    rs.getLong("twitter_user_id"),
                    rs.getLong("monthly_tweet_count"),
                    rs.getLong("last_tweet_id"),
                    rs.getLong("last_tweet_time"),
                    rs.getLong("month_start"),
                    rs.getBoolean("enabled")
                )
            )
        }

        it.resume(webhooks)
    }

    fun delete(guildId: Long, handle: String) {
        driverManager.executeUpdate(
            "DELETE FROM $table WHERE guild_id = ? AND handle = ?",
            guildId, handle
        )
    }
}

data class TwitterWebhook(
    val guildId: Long,
    val webhookUrl: String,
    val excludedTweetTypes: Set<TweetType>,
    val handle: String,
    val twitterUserId: Long,
    val monthlyTweetCount: Long,
    val lastTweetId: Long,
    val lastTweetTime: Long,
    val monthStart: Long,
    val enabled: Boolean
) {
    enum class TweetType(val id: Int) {
        TEXT_POST(0),
        MEDIA_POST(1),
        TEXT_AND_MEDIA_POST(2),
        REPLY(3),
        RETWEET(4);

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
}
