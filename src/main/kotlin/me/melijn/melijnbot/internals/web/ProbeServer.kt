package me.melijn.melijnbot.internals.web

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.utils.OS
import me.melijn.melijnbot.internals.utils.os
import me.melijn.melijnbot.internals.web.rest.shutdown.ShutdownResponseHandler
import net.dv8tion.jda.api.JDA
import java.util.concurrent.TimeUnit

class ProbeServer(container: Container) {

    private val server: NettyApplicationEngine = embeddedServer(Netty, if (os == OS.WIN) 11180 else 1180) { // HAHAHAAAAAHAAAAHAAHHAHAAHAHAHAHAAAAAAAA L:FDSFSAFSDfsdklhjnfsdjkflsdfgkljdsaljkfg fyu
        routing {
            get("/shutdown") {
                ShutdownResponseHandler.handleShutdownResponse(RequestContext(call, container), false)
                stop()
            }
            get("/ready") {
                if (MelijnBot.shardManager.shards.all { it.status == JDA.Status.CONNECTED || it.status == JDA.Status.LOADING_SUBSYSTEMS }) {
                    call.respondText(status = HttpStatusCode.OK) { "ready" }
                } else {
                    call.respondText(status = HttpStatusCode.InternalServerError) { "no ready" }
                }
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