package me.melijn.melijnbot.internals.services.reddit

import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commands.utility.MemeCommand
import me.melijn.melijnbot.commands.utility.RedditCommand
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import java.util.concurrent.TimeUnit

class RedditService(httpClient: HttpClient, driverManager: DriverManager) : Service("Reddit", 10, 1, TimeUnit.MINUTES) {
    override val service: RunnableTask = RunnableTask {
        for (subreddit in MemeCommand.subreddits) {
            if ((driverManager.redisConnection.async().ttl("reddit:posts:hot:$subreddit").await() ?: 0) < 500) {
                RedditCommand.requestPostsAndStore(httpClient, driverManager, subreddit, "hot", "day")
                delay(1000)
            }
        }
    }
}

class RedditAboutService(httpClient: HttpClient, driverManager: DriverManager) : Service("RedditAbout", 30, 2, TimeUnit.MINUTES) {
    override val service: RunnableTask = RunnableTask {
        for (subreddit in MemeCommand.subreddits) {
            if ((driverManager.redisConnection.async().ttl("reddit:posts:hot:$subreddit").await() ?: 0) < 1700) {
                RedditCommand.requestAboutAndStore(httpClient, driverManager, subreddit)
                delay(1000)
            }
        }
    }
}