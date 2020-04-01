package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class MusicNodeCommand : AbstractCommand("command.musicnode") {

    init {
        id = 143
        name = "musicNode"
        aliases = arrayOf("mn")
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.MUSIC
    }

    companion object {
        val map = mutableMapOf<Long, MusicNodeInfo>()
        val connectionMap = mutableMapOf<Long, Boolean>()
    }

    //Should support things like usa nodes and many more, should save in db, all connects should check cache
    override suspend fun execute(context: CommandContext) {
        when {
            context.args.isEmpty() -> {
                val currentNode = if (context.daoManager.musicNodeWrapper.isPremium(context.guildId)) {
                    "premium"
                } else {
                    "default"
                }
                val msg = context.getTranslation("$root.current")
                    .replace("%node%", currentNode)
                sendMsg(context, msg)
            }
            context.args[0] == "premium" -> {
                switchToPremiumNode(context)

                val msg = context.getTranslation("$root.selected")
                    .replace("%node%", "premium")
                sendMsg(context, msg)
            }
            context.args[0] == "default" -> {
                switchToDefaultNode(context)

                val msg = context.getTranslation("$root.selected")
                    .replace("%node%", "default")
                sendMsg(context, msg)
            }
            else -> {
                sendSyntax(context)
            }
        }
    }

    private suspend fun switchToPremiumNode(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val mNodeWrapper = context.daoManager.musicNodeWrapper
        val isPremium = mNodeWrapper.isPremium(context.guildId)
        if (isPremium) return
        connectionMap[context.guildId] = false
        mNodeWrapper.setNode(context.guildId, "premium")


        val track = trackManager.playingTrack ?: return

        val pos = trackManager.iPlayer.trackPosition
        val volume = trackManager.iPlayer.volume
        val channel = context.lavaManager.getConnectedChannel(context.guild) ?: return
        context.guildMusicPlayer.removeTrackManagerListener()
        trackManager.iPlayer = context.lavaManager.getIPlayer(context.guildId, true)
        context.guildMusicPlayer.addTrackManagerListener()

        context.lavaManager.closeConnectionLite(context.guildId, false)
        map[context.guildId] = MusicNodeInfo(channel.idLong, track, pos, volume, System.currentTimeMillis())
    }

    private suspend fun switchToDefaultNode(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val mNodeWrapper = context.daoManager.musicNodeWrapper
        val isPremium = mNodeWrapper.isPremium(context.guildId)
        if (!isPremium) return
        connectionMap[context.guildId] = true
        mNodeWrapper.setNode(context.guildId, "default")

        val track = trackManager.playingTrack ?: return

        val pos = trackManager.iPlayer.trackPosition
        val volume = trackManager.iPlayer.volume
        val channel = context.lavaManager.getConnectedChannel(context.guild) ?: return

        context.lavaManager.closeConnectionLite(context.guildId, true)
        map[context.guildId] = MusicNodeInfo(channel.idLong, track, pos, volume, System.currentTimeMillis())
    }
}

data class MusicNodeInfo(
    val channelId: Long,
    val track: AudioTrack,
    val position: Long,
    val volume: Int,
    val millis: Long
)