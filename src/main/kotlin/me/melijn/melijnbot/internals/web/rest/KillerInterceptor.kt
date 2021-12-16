package me.melijn.melijnbot.internals.web.rest

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.melijn.melijnbot.internals.threading.TaskManager
import okhttp3.Interceptor
import okhttp3.Response
import org.slf4j.LoggerFactory

class KillerInterceptor : Interceptor {

    val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        // route -> status code, count
        val map = mutableMapOf<String, MutableMap<Int, Int>>()
        var errorCounters = mutableMapOf<Int, Int>()
        val mutex = Mutex() // must be used for the above
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request);

        TaskManager.async {
            logger.info(String.format("Sending request %s on %s%n", request.url, chain.connection()))

            mutex.withLock {
                val route = request.url.encodedPath
                    .replace("\\d{17,22}".toRegex(), "ID")
                    .replace("interactions/ID/.*/callback", "/interactions/ID/INTERACTION_ID/callback")
                    .replace("/webhooks/ID/.*/messages.*", "/webhooks/ID/WEBHOOK_ID/messages")
                val current = map[route] ?: mutableMapOf()
                current[response.code] = (current[response.code] ?: 0) + 1
                errorCounters[response.code] = (errorCounters[response.code] ?: 0) + 1
            }

            logger.info(String.format("Received response for %s in ", response.request.url))
        }

        return response
    }
}