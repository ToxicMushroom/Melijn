package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.*
import me.melijn.melijnbot.objects.music.AudioLoader
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.translation.SC_SELECTOR
import me.melijn.melijnbot.objects.translation.YT_SELECTOR
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.entities.VoiceChannel

class PlayCommand : AbstractCommand("command.play") {

    init {
        id = 80
        name = "play"
        aliases = arrayOf("p")
        children = arrayOf(YTArg(root), SCArg(root), AttachmentArg(root))
        commandCategory = CommandCategory.MUSIC
        runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ)
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty() && context.getMessage().attachments.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val member = context.getMember() ?: return
        val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
        val lava: LavaManager = context.lavaManager

        val songArg = context.rawArg.trim()

        if (songArg.startsWith("https://") || songArg.startsWith("http://")) {
            if (!hasPermission(context, "$root.url")) {
                sendMissingPermissionMessage(context, "$root.url")
                return
            }
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            if (songArg.contains("open.spotify.com")) {
                spotifySearchNLoad(context.audioLoader, context, songArg)
            } else {
                context.audioLoader.loadNewTrackNMessage(context, songArg, true)
            }
        } else if (songArg.isNotBlank()) {
            if (!hasPermission(context, "$root.yt")) {
                sendMissingPermissionMessage(context, "$root.yt")
                return
            }
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return

            if (songArg.matches("spotify:(.*)".toRegex())) {
                spotifySearchNLoad(context.audioLoader, context, songArg)
            } else {
                context.audioLoader.loadNewTrackNMessage(context, "${YT_SELECTOR}$songArg", false)
            }
        } else {
            val tracks = context.getMessage().attachments.map { attachment -> attachment.url }
            if (!hasPermission(context, "$root.attachment")) {
                sendMissingPermissionMessage(context, "$root.attachment")
                return
            }
            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            for (url in tracks) {
                context.audioLoader.loadNewTrackNMessage(context, url, false)
            }
        }
    }

    class YTArg(root: String) : AbstractCommand("$root.yt") {

        init {
            name = "yt"
            aliases = arrayOf("youtube")
        }


        override suspend fun execute(context: CommandContext) {
            val member = context.getMember() ?: return
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val songArg = context.rawArg.trim()

            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            context.audioLoader.loadNewTrackNMessage(context, "${YT_SELECTOR}$songArg", false)
        }

    }

    class SCArg(root: String) : AbstractCommand("$root.sc") {

        init {
            name = "sc"
            aliases = arrayOf("soundcloud")
        }


        override suspend fun execute(context: CommandContext) {
            val member = context.getMember() ?: return
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val songArg = context.rawArg.trim()

            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            context.audioLoader.loadNewTrackNMessage(context, "${SC_SELECTOR}$songArg", false)
        }

    }

    class AttachmentArg(root: String) : AbstractCommand("$root.attachment") {

        init {
            name = "attachment"
            aliases = arrayOf("file")
        }


        override suspend fun execute(context: CommandContext) {
            if (context.getMessage().attachments.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }
            val member = context.getMember() ?: return
            val senderVoiceChannel: VoiceChannel = member.voiceState?.channel ?: return
            val lava: LavaManager = context.lavaManager

            val tracks = context.getMessage().attachments.map { attachment -> attachment.url }

            if (!lava.tryToConnectToVCNMessage(context, senderVoiceChannel)) return
            for (url in tracks) {
                context.audioLoader.loadNewTrackNMessage(context, url, false)
            }
        }

    }

    fun spotifySearchNLoad(audioLoader: AudioLoader, context: CommandContext, songArg: String) {
        context.webManager.getTracksFromSpotifyUrl(songArg,
            { track ->

            },
            { trackList ->

            },
            { simpleTrackList ->

            },
            { error ->

            }
        )
    }
}