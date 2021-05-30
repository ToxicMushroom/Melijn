package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.llklient.utils.LavalinkUtil
import me.melijn.melijnbot.commands.music.PlaylistCommand.AddArg.Companion.playlistsLimitReachedAndMessage
import me.melijn.melijnbot.commands.music.PlaylistCommand.AddArg.Companion.tracksLimitReachedAndMessage
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.RunConditionUtil
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable

class LikeCommand : AbstractCommand("command.like") {

    init {
        id = 225
        name = "like"
        aliases = arrayOf("favourite")
        runConditions = arrayOf(RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        val guildMusicPlayer = context.musicPlayerManager.getGuildMusicPlayer(context.guild)
        val audioTrack: AudioTrack = guildMusicPlayer.guildTrackManager.playingTrack ?: return

        if (context.args.isNotEmpty() &&
            !RunConditionUtil.checkPlayingTrackNotNull(context.container, context.message)
        ) {
            sendSyntax(context)
            return
        }

        val playlists = context.daoManager.playlistWrapper.getPlaylists(context.authorId)
        if (playlistsLimitReachedAndMessage(context, playlists.size)) {
            return
        }

        val tracksMap = getLikesPlaylist(context)
        if (tracksMap != null && tracksLimitReachedAndMessage(context, tracksMap.size)) {
            return
        }

        val position = tracksMap?.maxByOrNull { it.key }?.key ?: 0

        context.daoManager.playlistWrapper
            .set(context.authorId, "favourites", position + 1, LavalinkUtil.toMessage(audioTrack))

        val msg = context.getTranslation("$root.added")
            .withSafeVariable("title", audioTrack.info.title)
            .withVariable("position", (tracksMap?.size ?: 0) + 1)
        sendRsp(context, msg)
    }

    private suspend fun getLikesPlaylist(context: ICommandContext): Map<Int, String>? {
        val playlist = "favourites"
        val playlistsMap = context.daoManager.playlistWrapper.getPlaylists(context.authorId)
        return playlistsMap[playlist]
    }
}