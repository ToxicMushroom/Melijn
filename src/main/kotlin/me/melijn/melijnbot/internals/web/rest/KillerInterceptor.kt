package me.melijn.melijnbot.internals.web.rest

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class KillerInterceptor : Interceptor {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        // route -> status code, count
        val map = mutableMapOf<String, MutableMap<Int, Int>>()
        var errorCounters = mutableMapOf<Int, Int>()
        val mutex = Mutex() // must be used for the above
    }

    private val callbackInteraction = "interactions/ID/.*/callback".toRegex()
    private val webhookMessage = "/webhooks/ID/.*/messages.*".toRegex()
    private val reactionsRegex = "/reactions/(.+)".toRegex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request);


        TaskManager.async {
            logger.info("Sending request ${request.method} ${request.url}")

            mutex.withLock {
                val route = request.url.encodedPath
                    .replace(DISCORD_ID, "ID")
                    .replace(callbackInteraction, "/interactions/ID/INTERACTION_ID/callback")
                    .replace(webhookMessage, "/webhooks/ID/WEBHOOK_ID/messages")
                    .replace(reactionsRegex, "/reactions/EMOJI")
                val current = map[route] ?: mutableMapOf()
                current[response.code] = (current[response.code] ?: 0) + 1
                map[route] = current
                errorCounters[response.code] = (errorCounters[response.code] ?: 0) + 1
            }

            val msg = "Received ${response.code} status for ${request.method} ${request.url}\n"
            if (response.code > 399) logger.warn(msg)
            else logger.info(msg)
        }

        return response
    }
}