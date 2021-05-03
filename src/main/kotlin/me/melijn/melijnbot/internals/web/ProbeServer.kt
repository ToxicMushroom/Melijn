package me.melijn.melijnbot.internals.web

import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.web.rest.shutdown.ShutdownResponseHandler
import java.util.concurrent.TimeUnit

class ProbeServer(container: Container) {

    private val server: NettyApplicationEngine = embeddedServer(Netty, 1180) {
        routing {
            get("/shutdown") {
                ShutdownResponseHandler.handleShutdownResponse(RequestContext(call, container), false)
                stop()
            }
        }
    }

    fun stop() {
        server.stop(0, 2, TimeUnit.SECONDS)
    }

    fun start() {
        server.start(false)
    }
}