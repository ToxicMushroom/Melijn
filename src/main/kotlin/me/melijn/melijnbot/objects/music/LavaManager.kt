package me.melijn.melijnbot.objects.music

import lavalink.client.io.Link
import lavalink.client.io.jda.JdaLavalink
import lavalink.client.player.IPlayer
import lavalink.client.player.LavaplayerPlayerWrapper
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.notEnoughPermissionsAndMessage
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.sharding.ShardManager


class LavaManager(
    val lavalinkEnabled: Boolean,
    val daoManager: DaoManager,
    val shardManager: ShardManager,
    private val jdaLavaLink: JdaLavalink?,
    private val premiumLavaLink: JdaLavalink?
) {

    val musicPlayerManager: MusicPlayerManager = MusicPlayerManager(daoManager, this)

    fun getIPlayer(guildId: Long, premium: Boolean = false): IPlayer {
        val ll = if (premium) premiumLavaLink else jdaLavaLink
        return if (lavalinkEnabled && ll != null) {
            ll.getLink(guildId.toString()).player
        } else {
            LavaplayerPlayerWrapper(musicPlayerManager.getLPPlayer())
        }
    }

    fun openConnection(channel: VoiceChannel, premium: Boolean = false) {
        val ll = if (premium) premiumLavaLink else jdaLavaLink
        if (ll == null) {
            val selfMember = channel.guild.selfMember
            if (selfMember.hasPermission(channel, Permission.VOICE_CONNECT)) {
                channel.guild.audioManager.sendingHandler = AudioPlayerSendHandler(getIPlayer(channel.guild.idLong, premium))
                channel.guild.audioManager.openAudioConnection(channel)
            }
        } else {
            ll.getLink(channel.guild).connect(channel)
        }

        musicPlayerManager.getGuildMusicPlayer(channel.guild)
    }

    /**
     * @param context            This will be used to send replies
     * @param guild              This will be used to check permissions
     * @param channel This is the voice channel you want to join
     * @return returns true on success and false when failed
     */
    suspend fun tryToConnectToVCNMessage(context: CommandContext, channel: VoiceChannel, premium: Boolean = false): Boolean {
        if (notEnoughPermissionsAndMessage(context, channel, Permission.VOICE_CONNECT)) return false
        return if (channel.userLimit == 0 || channel.userLimit > channel.members.size || notEnoughPermissionsAndMessage(context, channel, Permission.VOICE_MOVE_OTHERS)) {
            openConnection(channel, premium)
            true
        } else {
            false
        }
    }

    fun tryToConnectToVCSilent(voiceChannel: VoiceChannel, premium: Boolean = false): Boolean {
        val guild: Guild = voiceChannel.guild
        if (!guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
            return false
        }

        return if (voiceChannel.userLimit == 0 || voiceChannel.userLimit > voiceChannel.members.size || guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_MOVE_OTHERS)) {
            openConnection(voiceChannel, premium)
            true
        } else {
            false
        }
    }

    fun closeConnection(guild: Guild, premium: Boolean = false) {
        val ll = if (premium) premiumLavaLink else jdaLavaLink

        if (ll == null) {
            guild.audioManager.closeAudioConnection()
        } else {
            ll.getLink(guild).disconnect()
        }

        musicPlayerManager.guildMusicPlayers.remove(guild.idLong)
    }

    fun closeConnection(guildId: Long, premium: Boolean = false) {
        val ll = if (premium) premiumLavaLink else jdaLavaLink
        val guild = shardManager.getGuildById(guildId)

        if (ll == null) {
            guild?.audioManager?.closeAudioConnection()
        } else {
            guild?.let { ll.getLink(it).disconnect() }
        }

        musicPlayerManager.guildMusicPlayers.remove(guildId)
    }

    fun isConnected(guild: Guild, premium: Boolean = false): Boolean {
        val ll = if (premium) premiumLavaLink else jdaLavaLink
        return if (ll == null) {
            guild.audioManager.isConnected
        } else {
            ll.getLink(guild).state == Link.State.CONNECTED
        }
    }

    fun getConnectedChannel(guild: Guild): VoiceChannel? = guild.selfMember.voiceState?.channel

}