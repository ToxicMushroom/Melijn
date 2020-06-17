package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.*
import me.melijn.melijnbot.objects.music.AudioLoader
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.translation.SC_SELECTOR
import me.melijn.melijnbot.objects.translation.YT_SELECTOR
import me.melijn.melijnbot.objects.utils.replacePrefix
import me.melijn.melijnbot.objects.utils.sendInGuild
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.entities.VoiceChannel

class PlayCommand : AbstractCommand("command.play") {

    init {
        id = 80
        name = "play"
        aliases = arrayOf("p")
        children = arrayOf(
            YTArg(root),
            SCArg(root),
            AttachmentArg(root)
        )
        runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
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


        val songPosition = if (context.args.isNotEmpty()) {
            when {
                args[0] == "-t" || args[0] == "-top" -> NextSongPosition.TOP
                args[0] == "-r" || args[0] == "-random" -> NextSongPosition.RANDOM
                args[0] == "-b" || args[0] == "-bottom" -> NextSongPosition.BOTTOM
                else -> {
                    songArg = context.rawArg.trim()
                    NextSongPosition.BOTTOM
                }
            }
        } else {
            songArg = context.rawArg.trim()
            NextSongPosition.BOTTOM
        }

        val premium = context.daoManager.musicNodeWrapper.isPremium(context.guildId)
        if (songArg.startsWith("https://") || songArg.startsWith("http://")) {
            if (!hasPermission(context, "$root.url")) {
                sendMissingPermissionMessage(context, "$root.url")
                return
            }
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return
            if (songArg.contains("open.spotify.com") && context.webManager.spotifyApi != null) {
                spotifySearchNLoad(context.audioLoader, context, songArg, songPosition)
            } else {
                context.audioLoader.loadNewTrackNMessage(context, songArg, true, songPosition)
            }
        } else if (songArg.isNotBlank()) {
            if (!hasPermission(context, "$root.yt")) {
                sendMissingPermissionMessage(context, "$root.yt")
                return
            }
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return

            if (songArg.matches("spotify:(.*)".toRegex()) && context.webManager.spotifyApi != null) {
                spotifySearchNLoad(context.audioLoader, context, songArg, songPosition)
            } else {
                context.audioLoader.loadNewTrackNMessage(context, "${YT_SELECTOR}$songArg", false, songPosition)
            }
        } else {
            val tracks = context.message.attachments.map { attachment -> attachment.url }
            if (!hasPermission(context, "$root.attachment")) {
                sendMissingPermissionMessage(context, "$root.attachment")
                return
            }
            if (botChannel == null && senderVoiceChannel != null && !lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return
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


        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val member = context.member
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            val songPosition = when {
                args[0] == "-t" || args[0] == "-top" -> NextSongPosition.TOP
                args[0] == "-r" || args[0] == "-random" -> NextSongPosition.RANDOM
                args[0] == "-b" || args[0] == "-bottom" -> NextSongPosition.BOTTOM
                else -> {
                    songArg = context.rawArg.trim()
                    NextSongPosition.BOTTOM
                }
            }

            val premium = context.daoManager.musicNodeWrapper.isPremium(context.guildId)
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return
            context.audioLoader.loadNewTrackNMessage(context, "${YT_SELECTOR}$songArg", false, songPosition)
        }

    }

    class SCArg(parent: String) : AbstractCommand("$parent.sc") {

        init {
            name = "sc"
            aliases = arrayOf("soundcloud")
        }


        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val member = context.member
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val args = context.oldArgs
            var songArg = context.getRawArgPart(1, -1)

            val songPosition = when {
                args[0] == "-t" || args[0] == "-top" -> NextSongPosition.TOP
                args[0] == "-r" || args[0] == "-random" -> NextSongPosition.RANDOM
                args[0] == "-b" || args[0] == "-bottom" -> NextSongPosition.BOTTOM
                else -> {
                    songArg = context.rawArg.trim()
                    NextSongPosition.BOTTOM
                }
            }

            val premium = context.daoManager.musicNodeWrapper.isPremium(context.guildId)
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return
            context.audioLoader.loadNewTrackNMessage(context, "${SC_SELECTOR}$songArg", false, songPosition)
        }
    }

    class AttachmentArg(parent: String) : AbstractCommand("$parent.attachment") {

        init {
            name = "attachment"
            aliases = arrayOf("file")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.message.attachments.isEmpty()) {
                sendSyntax(context)
                return
            }

            val member = context.member
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val args = context.args

            val songPosition = if (args.isNotEmpty()) {
                when {
                    args[0] == "-t" || args[0] == "-top" -> NextSongPosition.TOP
                    args[0] == "-r" || args[0] == "-random" -> NextSongPosition.RANDOM
                    args[0] == "-b" || args[0] == "-bottom" -> NextSongPosition.BOTTOM
                    else -> {
                        NextSongPosition.BOTTOM
                    }
                }
            } else {
                NextSongPosition.BOTTOM
            }


            val tracks = context.message.attachments.map { attachment -> attachment.url }

            val premium = context.daoManager.musicNodeWrapper.isPremium(context.guildId)
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel, premium)) return
            for (url in tracks) {
                context.audioLoader.loadNewTrackNMessage(context, url, false, songPosition)
            }
        }
    }

    private fun spotifySearchNLoad(audioLoader: AudioLoader, context: CommandContext, songArg: String, nextPos: NextSongPosition) {
        context.webManager.spotifyApi?.getTracksFromSpotifyUrl(songArg,
            { track ->
                audioLoader.loadSpotifyTrack(context, YT_SELECTOR + track.name, track.artists, track.durationMs, nextPos = nextPos)
            },
            { trackList ->
                audioLoader.loadSpotifyPlaylist(context, trackList, nextPos)
            },
            { simpleTrackList ->
                audioLoader.loadSpotifyAlbum(context, simpleTrackList, nextPos)
            },
            { error ->
                val msg = context.getTranslation("message.spotify.down")
                    .replacePrefix(context)
                sendMsg(context, msg)
                error.sendInGuild(context)
            }
        )
    }
}

enum class NextSongPosition {
    BOTTOM, RANDOM, TOP
}
