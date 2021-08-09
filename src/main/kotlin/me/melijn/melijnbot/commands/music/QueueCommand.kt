package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendInGuild
import me.melijn.melijnbot.internals.utils.message.sendMelijnMissingChannelPermissionMessage
import me.melijn.melijnbot.internals.utils.message.sendPaginationModularRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission

class QueueCommand : AbstractCommand("command.queue") {

    init {
        id = 82
        name = "queue"
        aliases = arrayOf("q", "list", "songlist", "songs", "tracks")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager

        var description = ""

        val cTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        if (cTrack == null) {
            val noSongPlaying = context.getTranslation("message.music.notracks")
            sendRsp(context, noSongPlaying)
            return
        }

        var totalDuration = cTrack.duration

        val status = context.getTranslation(if (trackManager.iPlayer.paused) "paused" else "playing")
        description += "[$status](${cTrack.info.uri}) - **%title%** `[${getDurationString(trackManager.iPlayer.trackPosition)} / ${
            getDurationString(
                cTrack.duration
            )
        }]`"
            .withSafeVariable("title", cTrack.info.title)

        val safeTracks = trackManager.tracks
        safeTracks.indexedForEach { index, track ->
            try {
                totalDuration += track.duration
                description += "\n[#${index + 1}](${track.info.uri}) - %title% `[${getDurationString(track.duration)}]`"
                    .withSafeVariable("title", track.info.title)
            } catch (t: Throwable) {
                t.sendInGuild(context, shouldSend = true, extra = (track.duration.toString() + "" + track.info))
            }
        }

        val title = context.getTranslation("$root.title")

        description += context.getTranslation("$root.fakefooter")
            .withVariable("duration", getDurationString(totalDuration - trackManager.iPlayer.trackPosition))
            .withVariable("amount", (safeTracks.size + 1).toString())

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
                        .withVariable("page", index + 1)
                        .withVariable("pages", queueParts.size)
                )
            }
            modularMessages.add(
                index, ModularMessage(
                    embed = eb.build()
                )
            )
        }

        if (modularMessages.size > 1) {
            if (context.selfMember.hasPermission(context.textChannel, Permission.MESSAGE_ADD_REACTION)) {
                sendPaginationModularRsp(context, modularMessages, 0)
            } else {
                sendMelijnMissingChannelPermissionMessage(context, listOf(Permission.MESSAGE_ADD_REACTION))
            }
        } else {
            sendRsp(context.textChannel, context, modularMessages.first())
        }
    }
}