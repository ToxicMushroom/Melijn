package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.utils.LavalinkUtil
import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.music.TrackUserData
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendPaginationModularMsg
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

const val tracksLimit = 25
const val playlistLimit = 3

const val premiumTrackLimit = 5000
const val premiumPlaylistLimit = 30

class PlaylistCommand : AbstractCommand("command.playlist") {

    init {
        id = 223
        name = "playlist"
        aliases = arrayOf("pl", "playlists")
        children = arrayOf(
            AddArg(root),
            SaveQueueArg(root),
            RemoveArg(root),
            ClearArg(root),
            ListArg(root),
            Rename(root),
            LoadArg(root)
        )
        commandCategory = CommandCategory.MUSIC
    }

    class Rename(parent: String) : AbstractCommand("$parent.rename") {

        init {
            name = "rename"
            aliases = arrayOf("changeName", "setName")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val tracksMap = getPlaylistByNameNMessage(context, 0) ?: return
            val previousName = tracksMap.name
            val newName = getStringFromArgsNMessage(context, 1, 1, 128) ?: return
            if (previousName == newName) {
                sendRsp(context, "... try something different")
                return
            }

            context.daoManager.playlistWrapper.rename(context.authorId, previousName, newName)

            val msg = context.getTranslation("$root.renamed")
                .withVariable("oldName", previousName)
                .withVariable("newName", newName)
            sendRsp(context, msg)
        }
    }

    class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

        init {
            name = "clear"
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val playlist = getPlaylistByNameNMessage(context, 0) ?: return
            val tracksMap = playlist.encodedTracks

            val trackValues = mutableListOf<String>()
            val tracks = tracksMap.toSortedMap()
                .map { it.value }
                .withIndex()
                .map {
                    trackValues.add(it.value)
                    LavalinkUtil.toAudioTrack(it.value)
                }

            context.daoManager.playlistWrapper.clear(context.authorId, playlist.name)

            val msg = context.getTranslation("$root.cleared")
                .withSafeVariable("playlist", playlist.name)
                .withVariable("count", tracks.size)

            sendRsp(context, msg)
        }

    }

    class SaveQueueArg(parent: String) : AbstractCommand("$parent.savequeue") {

        init {
            name = "saveQueue"
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)

            val playingTrack: AudioTrack = guildMusicPlayer.guildTrackManager.playingTrack ?: return
            var tracksToAdd = mutableListOf<AudioTrack>()
            tracksToAdd.add(playingTrack)
            guildMusicPlayer.guildTrackManager.tracks.forEach {
                tracksToAdd.add(it)
            }

            val playlistWrapper = context.daoManager.playlistWrapper
            val playlists = playlistWrapper.getPlaylists(context.authorId)
            if (AddArg.playlistsLimitReachedAndMessage(context, playlists.size)) {
                return
            }

            val freeSlots = getFreeSlots(context, playlists.size)
            if (tracksToAdd.size > freeSlots) {
                tracksToAdd = tracksToAdd.subList(0, freeSlots)
            }

            val playList = getPlaylistByNameN(context, 0)
            val tracksMap = playList?.encodedTracks
            if (tracksMap != null && AddArg.tracksLimitReachedAndMessage(context, tracksMap.size)) {
                return
            }

            val plName = playList?.name ?: (getStringFromArgsNMessage(context, 0, 1, 128) ?: return)
            var position = tracksMap?.maxByOrNull { it.key }?.key ?: 0

            tracksToAdd.forEach { track ->
                playlistWrapper.set(context.authorId, plName, ++position, LavalinkUtil.toMessage(track))
            }

            val msg = context.getTranslation("$root.added")
                .withVariable("amount", tracksToAdd.size)
                .withSafeVariable("playlist", plName)
                .withVariable("positionStart", (tracksMap?.size ?: 0) + 1)
                .withVariable("positionEnd", (tracksMap?.size ?: 0) + tracksToAdd.size)
            sendRsp(context, msg)
        }

        private suspend fun getFreeSlots(context: ICommandContext, size: Int): Int {
            val premium = context.daoManager.supporterWrapper.getUsers().contains(context.authorId)
            return if (premium) {
                premiumTrackLimit
            } else {
                tracksLimit
            } - size
        }

    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class LoadArg(parent: String) : AbstractCommand("$parent.load") {

        init {
            name = "load"
            aliases = arrayOf("l")
            runConditions = arrayOf(RunCondition.VC_BOT_OR_USER_DJ)
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val playlist = getPlaylistByNameNMessage(context, 0) ?: return
            val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)

            if (context.lavaManager.getConnectedChannel(context.guild) == null) {
                if (!RunConditionUtil.checkOtherBotAloneOrDJOrSameVC(
                        context.container,
                        context.message,
                        this,
                        context.getLanguage()
                    )
                ) return
                val vc = context.member.voiceState?.channel
                    ?: throw IllegalStateException("prob race condition")
                if (!context.lavaManager.tryToConnectToVCNMessage(context, vc, guildMusicPlayer.groupId)) return
            }

            val tracks = playlist.encodedTracks.toSortedMap().map {
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

        override suspend fun execute(context: ICommandContext) {
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
                val sb = StringBuilder("```INI\n# index - [name] - tracks")

                for ((index, playlistName) in playlistNames.withIndex()) {
                    sb.append("\n").append(index + 1).append(" - [").append(playlistName.escapeMarkdown())
                        .append("] - ")
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
            val tracks = tracksMap.encodedTracks.toSortedMap().map {
                val track = LavalinkUtil.toAudioTrack(it.value)
                track
            }

            val title = context.getTranslation("$root.tracks.title")
                .withSafeVariable("playlist", tracksMap.name)

            val sb = StringBuilder()
            for ((index, track) in tracks.withIndex()) {
                sb
                    .append("[#${index + 1}](")
                    .append(track.info.uri)
                    .append(") - ")
                    .append(track.info.title.escapeMarkdown())
                    .append(" `[")
                    .append(getDurationString(track.info.length))
                    .appendLine("]`")
            }

            val eb = Embedder(context)
                .setTitle(title)
            val parts = StringUtils.splitMessage(sb.toString())
            val msgs = mutableListOf<ModularMessage>()

            for ((index, part) in parts.withIndex()) {
                msgs.add(
                    ModularMessage(
                        embed = eb.setDescription(part)
                            .setFooter("Page ${index + 1}/${parts.size}")
                            .build()
                    )
                )
            }

            sendPaginationModularMsg(context, msgs, 0)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val playlist = getPlaylistByNameNMessage(context, 0) ?: return
            val tracksMap = playlist.encodedTracks
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

            context.daoManager.playlistWrapper.removeAll(context.authorId, playlist.name, absPositions)

            val msg = when {
                tracks.size > 1 -> {
                    context.getTranslation("$root.removed.multiple")
                        .withSafeVariable("playlist", playlist.name)
                        .withVariable("amount", tracks.size)
                }
                tracks.size == 1 -> {
                    context.getTranslation("$root.removed")
                        .withSafeVariable("playlist", playlist.name)
                        .withVariable("title", tracks.first().info.title.escapeMarkdown())
                }
                else -> {
                    sendRsp(context, "Removed nothing from ${playlist.name.escapeMarkdown().escapeDiscordInvites()}")
                    return
                }
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

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)
            val audioTrack: AudioTrack = guildMusicPlayer.guildTrackManager.playingTrack ?: return

            if (context.args.isNotEmpty() &&
                !RunConditionUtil.checkPlayingTrackNotNull(context.container, context.message)
            ) {
                sendSyntax(context)
                return
            }

            val playlistWrapper = context.daoManager.playlistWrapper
            val playlists = playlistWrapper.getPlaylists(context.authorId)
            if (playlistsLimitReachedAndMessage(context, playlists.size)) {
                return
            }

            val playlist = getPlaylistByNameN(context, 0)
            val tracksMap = playlist?.encodedTracks
            if (tracksMap != null && tracksLimitReachedAndMessage(context, tracksMap.size)) {
                return
            }
            val playlistName = playlist?.name ?: (getStringFromArgsNMessage(context, 0, 1, 128) ?: return)

            val position = tracksMap?.maxByOrNull { it.key }?.key ?: 0

            playlistWrapper.set(context.authorId, playlistName, position + 1, LavalinkUtil.toMessage(audioTrack))

            val msg = context.getTranslation("$root.added")
                .withSafeVariable("title", audioTrack.info.title)
                .withSafeVariable("playlist", playlistName)
                .withVariable("position", (tracksMap?.size ?: 0) + 1)
            sendRsp(context, msg)
        }

        companion object {
            suspend fun tracksLimitReachedAndMessage(context: ICommandContext, size: Int): Boolean {
                if (size < tracksLimit) return false

                val premium = context.daoManager.supporterWrapper.getUsers().contains(context.authorId)
                if (premium && size < premiumTrackLimit) {
                    return false
                }

                val root = context.commandOrder.first().root
                val msg = if (premium) {
                    context.getTranslation("$root.tracklimit.premium")
                } else {
                    context.getTranslation("$root.tracklimit")
                }
                sendRsp(context, msg)
                return true
            }

            suspend fun playlistsLimitReachedAndMessage(context: ICommandContext, size: Int): Boolean {
                if (size < playlistLimit) return false

                val premium = context.daoManager.supporterWrapper.getUsers().contains(context.authorId)
                if (premium && size < premiumPlaylistLimit) {
                    return false
                }
                val root = context.commandOrder.first().root
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
        private suspend fun getPlaylistByNameN(context: ICommandContext, index: Int): Playlist? {
            val playlist = getStringFromArgsNMessage(context, index, 1, 128) ?: return null
            val plIndex = playlist.toIntOrNull()
            val playlistsMap = context.daoManager.playlistWrapper.getPlaylists(context.authorId)
            val indexedName = plIndex?.takeIf {
                (it - 1) < playlistsMap.size && (it - 1) >= 0
            }?.let {
                playlistsMap.keys.sorted()[it - 1]
            }
            val indexed = indexedName?.let { playlistsMap[indexedName] }

            return if (playlistsMap.containsKey(playlist)) {
                Playlist(
                    playlist,
                    playlistsMap[playlist] ?: return null
                )
            } else {
                Playlist(
                    indexedName ?: return null,
                    indexed ?: return null
                )
            }
        }

        private suspend fun getPlaylistByNameNMessage(context: ICommandContext, index: Int): Playlist? {
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

data class Playlist(
    val name: String,
    val encodedTracks: Map<Index, EncodedTrack>
)

typealias Index = Int
typealias EncodedTrack = String