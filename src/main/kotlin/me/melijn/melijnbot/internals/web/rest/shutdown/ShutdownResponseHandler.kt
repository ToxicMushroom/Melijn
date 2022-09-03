package me.melijn.melijnbot.internals.web.rest.shutdown

import io.ktor.response.*
import kotlinx.coroutines.delay
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.web.RequestContext

object ShutdownResponseHandler {
    suspend fun handleShutdownResponse(context: RequestContext, reqAuth: Boolean = true): Boolean {
        val container = context.container
        val call = context.call

        val job = TaskManager.async {
            MelijnBot.shardManager.shutdown()
        }

        var seconds = 10
        while (!job.isCompleted && seconds != 0) {
            delay(1000)
            seconds--
        }

        call.respondText { "Shutdown complete!" }
        return true
    }
}