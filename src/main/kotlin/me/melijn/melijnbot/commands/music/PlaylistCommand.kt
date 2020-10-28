package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.utils.LavalinkUtil
import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.music.TrackUserData
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission

const val tracksLimit = 20
const val playlistLimit = 3

const val premiumTrackLimit = 200
const val premiumPlaylistLimit = 20

class PlaylistCommand : AbstractCommand("command.playlist") {

    init {
        id = 223
        name = "playlist"
        aliases = arrayOf("pl", "playlists")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root),
            LoadArg(root)
        )
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class LoadArg(parent: String) : AbstractCommand("$parent.load") {

        init {
            name = "load"
            aliases = arrayOf("l")
            runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ)
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val tracksMap = getPlaylistByNameNMessage(context, 0) ?: return
            val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)

            if (context.lavaManager.getConnectedChannel(context.guild) == null) {
                if (!RunConditionUtil.checkOtherBotAloneOrDJOrSameVC(context.container, context.event, this, context.getLanguage())) return
                val vc = context.member.voiceState?.channel ?: throw IllegalStateException("I messed up")
                if (notEnoughPermissionsAndMessage(context, vc, Permission.VOICE_SPEAK, Permission.VOICE_CONNECT)) return

                context.lavaManager.openConnection(vc, context.getGuildMusicPlayer().groupId)
            }

            val tracks = tracksMap.toSortedMap().map {
                LavalinkUtil.toAudioTrack(it.value)
            }
            var notAdded = 0
            for (track in tracks) {
                track.userData = TrackUserData(context.author)

                if (!guildMusicPlayer.safeQueueSilent(context.daoManager, track, NextSongPosition.BOTTOM)) notAdded++
                else {
                    LogUtils.addMusicPlayerNewTrack(context, track)
                }
            }

            val msg = context.getTranslation("$root.loaded")
                .withVariable("loadedAmount", tracks.size - notAdded)
                .withVariable("failedAmount", notAdded)
            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                val playlists = context.daoManager.playlistWrapper.getPlaylists(context.authorId)
                if (playlists.isEmpty()) {
                    val msg = context.getTranslation("$root.playlist.empty")
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                    sendRsp(context, msg)
                    return
                }

                val playlistNames = playlists.keys.sorted()

                val title = context.getTranslation("$root.playlist.title")
                val sb = StringBuilder("```INI\n# [name] - tracks")


                for (playlistName in playlistNames) {
                    sb.append("\n[").append(playlistName.escapeMarkdown()).append("] - ")
                        .append(playlists[playlistName]?.size ?: 0)
                }
                sb.append("```")
                val eb = Embedder(context)
                    .setTitle(title)
                    .setDescription(sb.toString())
                sendEmbedRsp(context, eb.build())
                return
            }

            val tracksMap = getPlaylistByNameNMessage(context, 0) ?: return
            val tracks = tracksMap.toSortedMap().map {
                val track = LavalinkUtil.toAudioTrack(it.value)
                track
            }

            val title = context.getTranslation("$root.tracks.title")
                .withSafeVariable("playlist", context.args[0])


            val sb = StringBuilder()
            for ((index, track) in tracks.withIndex()) {
                sb
                    .append("\n[#${index + 1}](")
                    .append(track.info.uri)
                    .append(") - ")
                    .append(track.info.title.escapeMarkdown())
                    .append(" `[")
                    .append(getDurationString(track.info.length))
                    .appendLine("]`")
            }
            val eb = Embedder(context)
                .setTitle(title)
                .setDescription(sb.toString())
            sendEmbedRsp(context, eb.build())
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val tracksMap = getPlaylistByNameNMessage(context, 0) ?: return
            val position = getIntegersFromArgsNMessage(context, 1, 1, tracksMap.size) ?: return

            val trackValues = mutableListOf<String>()
            val tracks = tracksMap.toSortedMap()
                .map { it.value }
                .withIndex()
                .filter { position.contains(it.index) }
                .map {
                    trackValues.add(it.value)
                    LavalinkUtil.toAudioTrack(it.value)
                }
            val absPositions = tracksMap.entries
                .filter { trackValues.contains(it.value) }
                .map { it.key }

            context.daoManager.playlistWrapper.removeAll(context.authorId, context.args[0], absPositions)

            val msg = if (tracks.size > 1) {
                context.getTranslation("$root.removed.multiple")
                    .withSafeVariable("playlist", context.args[0])
                    .withVariable("amount", tracks.size)
            } else {
                context.getTranslation("$root.removed")
                    .withSafeVariable("playlist", context.args[0])
                    .withVariable("title", tracks.first().info.title.escapeMarkdown())
            }

            sendRsp(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("append", "addPlaying", "a")
            runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)
            val audioTrack: AudioTrack = guildMusicPlayer.guildTrackManager.playingTrack ?: return

            if (context.args.isNotEmpty() &&
                !RunConditionUtil.checkPlayingTrackNotNull(context.container, context.event)) {
                sendSyntax(context)
                return
            }

            val playlists = context.daoManager.playlistWrapper.getPlaylists(context.authorId)
            if (playlistsLimitReachedAndMessage(context, playlists.size)) {
                return
            }


            val tracksMap = getPlaylistByNameN(context, 0)
            if (tracksMap != null && tracksLimitReachedAndMessage(context, tracksMap.size)) {
                return
            }

            val position = tracksMap?.maxByOrNull { it.key }?.key ?: 0

            context.daoManager.playlistWrapper
                .set(context.authorId, context.args[0], position+1, LavalinkUtil.toMessage(audioTrack))

            val msg = context.getTranslation("$root.added")
                .withSafeVariable("title", audioTrack.info.title)
                .withSafeVariable("playlist", context.args[0])
                .withVariable("position", (tracksMap?.size ?: 0) + 1)
            sendRsp(context, msg)
        }

        companion object {
            suspend fun tracksLimitReachedAndMessage(context: CommandContext, size: Int): Boolean {
                if (size < tracksLimit) return false

                val premium = context.daoManager.supporterWrapper.getUsers().contains(context.authorId)
                if (premium && size < premiumTrackLimit) {
                    return false
                }

                val root = context.commandOrder.last().root
                val msg = if (premium) {
                    context.getTranslation("$root.tracklimit.premium")
                } else {
                    context.getTranslation("$root.tracklimit")
                }
                sendRsp(context, msg)
                return true
            }

            suspend fun playlistsLimitReachedAndMessage(context: CommandContext, size: Int): Boolean {
                if (size < playlistLimit) return false

                val premium = context.daoManager.supporterWrapper.getUsers().contains(context.authorId)
                if (premium && size < premiumPlaylistLimit) {
                    return false
                }
                val root = context.commandOrder.last().root
                val msg = if (premium) {
                    context.getTranslation("$root.playlistlimit.premium")
                } else {
                    context.getTranslation("$root.playlistlimit")
                }
                sendRsp(context, msg)
                return true
            }
        }
    }

    companion object {
        private suspend fun getPlaylistByNameN(context: CommandContext, index: Int): Map<Int, String>? {
            val playlist = getStringFromArgsNMessage(context, index, 1, 128) ?: return null
            val playlistsMap = context.daoManager.playlistWrapper.getPlaylists(context.authorId)
            return playlistsMap[playlist]
        }

        private suspend fun getPlaylistByNameNMessage(context: CommandContext, index: Int): Map<Int, String>? {
            val tracksMap = getPlaylistByNameN(context, index)
            val playlist = context.args[index]

            if (tracksMap == null) {
                val msg = context.getTranslation(context.commandOrder.first().root + ".unknownplaylist")
                    .withSafeVariable(PLACEHOLDER_ARG, playlist)
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
            }

            return tracksMap
        }
    }
}