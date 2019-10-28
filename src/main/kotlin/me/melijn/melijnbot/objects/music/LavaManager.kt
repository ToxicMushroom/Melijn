package me.melijn.melijnbot.objects.music

import lavalink.client.io.Link
import lavalink.client.io.jda.JdaLavalink
import lavalink.client.player.IPlayer
import lavalink.client.player.LavaplayerPlayerWrapper
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.sharding.ShardManager


class LavaManager(
    val lavalinkEnabled: Boolean,
    val daoManager: DaoManager,
    val shardManager: ShardManager,
    private val jdaLavaLink: JdaLavalink?
) {

    val musicPlayerManager: MusicPlayerManager = MusicPlayerManager(this)

    fun getIPlayer(guildId: Long): IPlayer {
        return if (lavalinkEnabled && jdaLavaLink != null) {
            jdaLavaLink.getLink(guildId.toString()).player
        } else {
            LavaplayerPlayerWrapper(musicPlayerManager.getLPPlayer())
        }
    }

    fun openConnection(channel: VoiceChannel) {
        if (jdaLavaLink == null) {
            val selfMember = channel.guild.selfMember
            if (selfMember.hasPermission(channel, Permission.VOICE_CONNECT)) {
                channel.guild.audioManager.sendingHandler = AudioPlayerSendHandler(getIPlayer(channel.guild.idLong))
                channel.guild.audioManager.openAudioConnection(channel)
            }
        } else jdaLavaLink.getLink(channel.guild).connect(channel)
    }

    /**
     * @param context            This will be used to send replies
     * @param guild              This will be used to check permissions
     * @param senderVoiceChannel This is the voice channel you want to join
     * @return returns true on success and false when failed
     */
    fun tryToConnectToVCNMessage(context: CommandContext, senderVoiceChannel: VoiceChannel): Boolean {
        val guild = senderVoiceChannel.guild
        if (!guild.selfMember.hasPermission(senderVoiceChannel, Permission.VOICE_CONNECT)) {
            context.reply("I don't have permission to join your Voice Channel")
            return false
        }
        return if (senderVoiceChannel.userLimit == 0 || senderVoiceChannel.userLimit > senderVoiceChannel.members.size || guild.selfMember.hasPermission(senderVoiceChannel, Permission.VOICE_MOVE_OTHERS)) {
            openConnection(senderVoiceChannel)
            true
        } else {
            context.reply("Your channel is full. I need the **Move Members** permission to join full channels")
            false
        }
    }

    fun tryToConnectToVCSilent(voiceChannel: VoiceChannel): Boolean {
        val guild: Guild = voiceChannel.guild
        if (!guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
            return false
        }
        return if (voiceChannel.userLimit == 0 || voiceChannel.userLimit > voiceChannel.members.size || guild.selfMember.hasPermission(voiceChannel, Permission.VOICE_MOVE_OTHERS)) {
            openConnection(voiceChannel)
            true
        } else {
            false
        }
    }

    fun closeConnection(guild: Guild) {
        if (jdaLavaLink == null) {
            guild.audioManager.closeAudioConnection()
        } else jdaLavaLink.getLink(guild).disconnect()
    }

    fun closeConnection(guildId: Long) {
        val guild = shardManager.getGuildById(guildId)
        if (jdaLavaLink == null) {
            guild?.audioManager?.closeAudioConnection()
        } else jdaLavaLink.getLink(guild).disconnect()
    }

    fun isConnected(guild: Guild): Boolean {
        return if (jdaLavaLink == null) {
            guild.audioManager.isConnected
        } else jdaLavaLink.getLink(guild).state == Link.State.CONNECTED
    }

    fun getConnectedChannel(guild: Guild): VoiceChannel? = guild.selfMember.voiceState?.channel

}