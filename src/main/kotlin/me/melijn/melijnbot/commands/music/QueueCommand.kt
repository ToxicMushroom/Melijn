package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.Permission

class QueueCommand : AbstractCommand("command.queue") {

    init {
        id = 82
        name = "queue"
        aliases = arrayOf("q", "list", "songlist", "songs", "tracks")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val trackManager = context.musicPlayerManager.getGuildMusicPlayer(context.guild).guildTrackManager
        val allTracks = trackManager.tracks.toMutableList()

        var description = ""


        val cTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        if (cTrack == null) {
            val noSongPlaying = context.getTranslation("message.music.notracks")
            sendMsg(context, noSongPlaying)
            return
        }

        var totalDuration = cTrack.duration

        val status = context.getTranslation(if (trackManager.iPlayer.isPaused) "paused" else "playing")
        description += "[$status](${cTrack.info.uri}) - **${cTrack.info.title}** `[${getDurationString(trackManager.iPlayer.trackPosition)} / ${getDurationString(cTrack.duration)}]`"
        for ((index, track) in allTracks.withIndex()) {
            totalDuration += track.duration
            description += "\n[#${index + 1}](${track.info.uri}) - ${track.info.title} `[${getDurationString(track.duration)}]`"
        }

        val title = context.getTranslation("$root.title")

        description += context.getTranslation("$root.fakefooter")
            .replace("%duration%", getDurationString(totalDuration - trackManager.iPlayer.trackPosition))
            .replace("%amount%", (allTracks.size + 1).toString())

        val footerPagination = context.getTranslation("message.pagination")

        val modularMessages = mutableListOf<ModularMessage>()

        val queueParts = StringUtils.splitMessage(description)
        val eb = Embedder(context)
        for ((index, queue) in queueParts.withIndex()) {
            eb.setTitle(if (index == 0) title else null)
            eb.setDescription(queue)
            if (queueParts.size > 1) {
                eb.setFooter(
                    footerPagination
                        .replace("%page%", index + 1)
                        .replace("%pages%", queueParts.size)
                )
            }
            modularMessages.add(index, ModularMessage(
                embed = eb.build()
            ))
        }

        if (modularMessages.size > 1) {
            sendPaginationModularMsg(context, modularMessages, 0)
        } else {
            sendMsg(context.textChannel, modularMessages.first())
        }
    }
}