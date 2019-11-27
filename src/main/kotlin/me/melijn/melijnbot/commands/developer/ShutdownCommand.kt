package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.system.exitProcess


class ShutdownCommand : AbstractCommand("command.shutdown") {

    init {
        id = 123
        name = "shutdown"

        commandCategory = CommandCategory.DEVELOPER
    }

    val logger = LoggerFactory.getLogger(this::class.java.name)

    override suspend fun execute(context: CommandContext) {
        val players = context.lavaManager.musicPlayerManager.getPlayers()
        val file = getResourceAsFile("Melijn.mp3") ?: throw IllegalArgumentException("ree")
        val audioTrack = context.audioLoader.localTrackToAudioTrack(file.absolutePath)
        if (audioTrack == null) {
            logger.error("Failed to load audio track for shutdown sound.")
            return
        }
        val wrapper = context.daoManager.tracksWrapper
        for ((guildId, player) in players) {
            val guild = context.shardManager.getGuildById(guildId) ?: return
            val channel = context.lavaManager.getConnectedChannel(guild) ?: return
            val trackManager = player.guildTrackManager
            val pTrack = trackManager.playingTrack ?: continue
            wrapper.put(guildId, pTrack, trackManager.tracks)
            wrapper.addChannel(guildId, channel.idLong)

            trackManager.stopAndDestroy()
        }

        sendMsg(context, "Shutting down")
        context.container.shuttingDown = true
        context.taskManager.asyncAfter(3_000) {
            exitProcess(0)
        }
    }
}

fun getResourceAsFile(resourcePath: String): File? {
    return try {
        val inStream = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath) ?: return null
        val tempFile = File.createTempFile(inStream.hashCode().toString(), ".tmp")
        tempFile.deleteOnExit()
        FileOutputStream(tempFile).use { out ->
            //copy stream
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
        }
        tempFile
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}