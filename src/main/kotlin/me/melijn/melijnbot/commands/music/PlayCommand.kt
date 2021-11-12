package me.melijn.melijnbot.commands.music

import com.wrapper.spotify.exceptions.detailed.NotFoundException
import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.music.AudioLoader
import me.melijn.melijnbot.internals.music.LavaManager
import me.melijn.melijnbot.internals.translation.SC_SELECTOR
import me.melijn.melijnbot.internals.translation.YT_SELECTOR
import me.melijn.melijnbot.internals.utils.message.sendInGuild
import me.melijn.melijnbot.internals.utils.message.sendMissingPermissionMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.replacePrefix
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.utils.MarkdownSanitizer

val spotifyURIRegex = Regex("spotify:(\\w+):(\\w+)")

class PlayCommand : AbstractCommand("command.play") {

    init {
        id = 80
        name = "play"
        aliases = arrayOf("p")
        children = arrayOf(
//            YTArg(root),
            SCArg(root),
            AttachmentArg(root)
        )
        runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        if ((context.args.isEmpty() || context.rawArg.isBlank()) && context.message.attachments.isEmpty()) {
            sendSyntax(context)
            return
        }

        val member = context.member
        val senderVoiceChannel = member.voiceState?.channel
        val botChannel = context.lavaManager.getConnectedChannel(context.guild)
        if (senderVoiceChannel == null && botChannel == null) throw IllegalArgumentException("Fix vc_bot_or_user_dj")
        val lava: LavaManager = context.lavaManager

        val args = context.oldArgs
        var songArg = context.getRawArgPart(1, -1)

        var songPosition = if (context.args.isNotEmpty()) {
            NextSongPosition.getPosByTrigger(args[0])
        } else null
        if (songPosition == null) {
            songArg = context.fullArg.trim()
            songPosition = NextSongPosition.BOTTOM
        }

        val groupId = context.getGuildMusicPlayer().groupId
        if (songArg.startsWith("https://") || songArg.startsWith("http://") || songArg.endsWith(">") && (
                songArg.startsWith("<https://") || songArg.startsWith("<http://"))
        ) {
            songArg = songArg.removeSurrounding("<", ">")
            if (!hasPermission(context, "play.url")) {
                sendMissingPermissionMessage(context, "play.url")
                return
            }
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(
                    context,
                    senderVoiceChannel,
                    groupId
                )
            ) return
            if (songArg.contains("open.spotify.com") && context.webManager.spotifyApi != null) {
                spotifySearchNLoad(context.audioLoader, context, songArg, songPosition)
            } else {
                context.audioLoader.loadNewTrackNMessage(context, songArg, true, songPosition)
            }
        } else if (songArg.isNotBlank()) {
            if (!hasPermission(context, "play.yt")) {
                sendMissingPermissionMessage(context, "play.yt")
                return
            }
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(
                    context,
                    senderVoiceChannel,
                    groupId
                )
            ) return

            if (spotifyURIRegex.matches(songArg) && context.webManager.spotifyApi != null) {
                spotifySearchNLoad(context.audioLoader, context, songArg, songPosition)
            } else {
                context.audioLoader.loadNewTrackNMessage(context, "${YT_SELECTOR}$songArg", false, songPosition)
            }
        } else {
            val tracks = context.message.attachments.map { attachment -> attachment.url }
            if (!hasPermission(context, "play.attachment")) {
                sendMissingPermissionMessage(context, "play.attachment")
                return
            }
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(
                    context,
                    senderVoiceChannel,
                    groupId
                )
            ) return
            for (url in tracks) {
                context.audioLoader.loadNewTrackNMessage(context, url, false, songPosition)
            }
        }
    }

    class YTArg(parent: String) : AbstractCommand("$parent.yt") {

        init {
            name = "yt"
            aliases = arrayOf("youtube")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val member = context.member
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            var songPosition = NextSongPosition.getPosByTrigger(args[0])
            if (songPosition == null) {
                songArg = context.rawArg.trim()
                songPosition = NextSongPosition.BOTTOM
            }

            val groupId = context.getGuildMusicPlayer().groupId
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel, groupId)) return
            context.audioLoader.loadNewTrackNMessage(context, "${YT_SELECTOR}$songArg", false, songPosition)
        }

    }

    class SCArg(parent: String) : AbstractCommand("$parent.sc") {

        init {
            name = "sc"
            aliases = arrayOf("soundcloud")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val member = context.member
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            var songPosition = NextSongPosition.getPosByTrigger(args[0])
            if (songPosition == null) {
                songArg = context.rawArg.trim()
                songPosition = NextSongPosition.BOTTOM
            }

            val groupId = context.getGuildMusicPlayer().groupId
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel, groupId)) return
            context.audioLoader.loadNewTrackNMessage(context, "${SC_SELECTOR}$songArg", false, songPosition)
        }
    }

    class AttachmentArg(parent: String) : AbstractCommand("$parent.attachment") {

        init {
            name = "attachment"
            aliases = arrayOf("file")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.message.attachments.isEmpty()) {
                sendSyntax(context)
                return
            }

            val member = context.member
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val args = context.args

            val songPosition = if (args.isNotEmpty()) {
                NextSongPosition.getPosByTrigger(args[0]) ?: NextSongPosition.BOTTOM
            } else {
                NextSongPosition.BOTTOM
            }

            val tracks = context.message.attachments.map { attachment -> attachment.url }

            val groupId = context.getGuildMusicPlayer().groupId
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel, groupId)) return
            for (url in tracks) {
                context.audioLoader.loadNewTrackNMessage(context, url, false, songPosition)
            }
        }
    }

    private suspend fun spotifySearchNLoad(
        audioLoader: AudioLoader,
        context: ICommandContext,
        songArg: String,
        nextPos: NextSongPosition
    ) {
        context.webManager.spotifyApi?.getTracksFromSpotifyUrl(songArg,
            { track ->
                audioLoader.loadSpotifyTrack(
                    context,
                    YT_SELECTOR + track.name,
                    track.artists,
                    track.durationMs,
                    nextPos = nextPos
                )
            },
            { trackList ->
                audioLoader.loadSpotifyPlaylist(context, trackList, nextPos)
            },
            { simpleTrackList ->
                audioLoader.loadSpotifyAlbum(context, simpleTrackList, nextPos)
            },
            { error ->
                when (error) {
                    is NotFoundException -> {
                        val msg = context.getTranslation("message.spotify.notfound")
                            .withVariable("url", MarkdownSanitizer.escape(context.fullArg))
                        sendRsp(context, msg)
                    }
                    is IllegalArgumentException -> {
                        val msg = context.getTranslation("message.spotify.unknownlink")
                            .withVariable("url", MarkdownSanitizer.escape(context.fullArg))
                        sendRsp(context, msg)
                    }
                    else -> {
                        val msg = context.getTranslation("message.spotify.down")
                            .replacePrefix(context)
                        sendRsp(context, msg)
                        error.sendInGuild(context)
                    }
                }
            }
        )
    }
}

enum class NextSongPosition(val triggers: Array<String>) {
    BOTTOM(arrayOf("b", "bottom")),
    RANDOM(arrayOf("r", "random")),
    TOP(arrayOf("t", "top")),
    TOPSKIP(arrayOf("ts", "topskip"));

    companion object {
        fun getPosByTrigger(trigger: String): NextSongPosition? {
            return values().firstOrNull { pos ->
                pos.triggers.any { ("-$it").equals(trigger, true) }
            }
        }
    }
}
