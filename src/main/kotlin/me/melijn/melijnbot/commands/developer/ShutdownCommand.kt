package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.services.voice.VOICE_SAFE
import me.melijn.melijnbot.objects.utils.sendMsg
import kotlin.system.exitProcess


class ShutdownCommand : AbstractCommand("command.shutdown") {

    init {
        id = 123
        name = "shutdown"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val players = context.lavaManager.musicPlayerManager.getPlayers()
        val wrapper = context.daoManager.tracksWrapper

        for ((guildId, player) in HashMap(players)) {
            val guild = context.shardManager.getGuildById(guildId) ?: continue
            val channel = context.lavaManager.getConnectedChannel(guild) ?: continue
            val trackManager = player.guildTrackManager
            val pTrack = trackManager.playingTrack ?: continue

            pTrack.position = trackManager.iPlayer.trackPosition

            wrapper.put(guildId, context.selfUser.idLong, pTrack, trackManager.tracks)
            wrapper.addChannel(guildId, channel.idLong)

            VOICE_SAFE.acquire()
            trackManager.stopAndDestroy()
            VOICE_SAFE.release()
        }

        sendMsg(context, "Shutting down")
        context.container.shuttingDown = true
        context.taskManager.asyncAfter(3_000) {
            exitProcess(0)
        }
    }
}