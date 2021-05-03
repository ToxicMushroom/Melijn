package me.melijn.melijnbot.internals.web.rest.shutdown

import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.web.RequestContext

object ShutdownResponseHandler {
    suspend fun handleShutdownResponse(context: RequestContext, reqAuth: Boolean = true) {
        val container = context.container
        val call = context.call
        val players = container.lavaManager.musicPlayerManager.getPlayers()
        val wrapper = container.daoManager.tracksWrapper

        if (reqAuth && call.request.header("Authorization") != context.restToken) {
            call.respondText(status = HttpStatusCode.Forbidden) { "Invalid token\n" }
            return
        }

        container.shuttingDown = true

        for ((guildId, player) in HashMap(players)) {
            val guild = MelijnBot.shardManager.getGuildById(guildId) ?: continue
            val channel = context.lavaManager.getConnectedChannel(guild) ?: continue
            val trackManager = player.guildTrackManager
            val pTrack = trackManager.playingTrack ?: continue

            pTrack.position = trackManager.iPlayer.trackPosition

            wrapper.put(guildId, container.settings.botInfo.id, pTrack, trackManager.tracks)
            wrapper.addChannel(guildId, channel.idLong)

            trackManager.stopAndDestroy()
        }

        call.respondText { "Shutdown complete!" }
    }
}