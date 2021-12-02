package me.melijn.melijnbot.internals.music

import me.melijn.llklient.io.jda.JDALavalink
import me.melijn.llklient.player.IPlayer
import me.melijn.llklient.player.LavaplayerPlayerWrapper
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.isPremiumGuild
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.notEnoughPermissionsAndMessage
import me.melijn.melijnbot.internals.utils.replacePrefix
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LavaManager(
    val lavalinkEnabled: Boolean,
    val daoManager: DaoManager,
    val jdaLavaLink: JDALavalink?
) {

    val musicPlayerManager: MusicPlayerManager = MusicPlayerManager(daoManager, this)
    val logger: Logger = LoggerFactory.getLogger("LavaManager")

    fun getIPlayer(guildId: Long, groupId: String): IPlayer {
        return if (lavalinkEnabled && jdaLavaLink != null) {
            jdaLavaLink.getLink(guildId, groupId).player
        } else {
            LavaplayerPlayerWrapper(musicPlayerManager.getLPPlayer())
        }
    }

    fun openConnection(channel: VoiceChannel, groupId: String) {
        if (jdaLavaLink == null) {
            val selfMember = channel.guild.selfMember
            if (selfMember.hasPermission(channel, Permission.VOICE_CONNECT)) {
                channel.guild.audioManager.sendingHandler =
                    AudioPlayerSendHandler(getIPlayer(channel.guild.idLong, groupId))
                channel.guild.audioManager.openAudioConnection(channel)
            }
        } else {
            jdaLavaLink.getLink(channel.guild.idLong, groupId).connect(channel)
        }
    }

    /**
     * @param context            This will be used to send replies
     * @param channel This is the voice channel you want to join
     * @return returns true on success and false when failed
     */
    suspend fun tryToConnectToVCNMessage(context: ICommandContext, channel: VoiceChannel, groupId: String): Boolean {
        if (notEnoughPermissionsAndMessage(context, channel, Permission.VOICE_CONNECT)) return false
        val canJoin = (channel.userLimit == 0 || channel.userLimit > channel.members.size) || !notEnoughPermissionsAndMessage(
            context,
            channel,
            Permission.VOICE_MOVE_OTHERS
        )
        val hasPremiumUsers = hasPremiumUserElseMessage(context, channel)
        return if (hasPremiumUsers && canJoin) {
            openConnection(channel, groupId)
            true
        } else {
            false
        }
    }

    suspend fun tryToConnectToVCSilent(channel: VoiceChannel, groupId: String): Boolean {
        val guild: Guild = channel.guild
        if (!guild.selfMember.hasPermission(channel, Permission.VOICE_CONNECT)) return false
        val dao = Container.instance.daoManager
        val channelNotFull = channel.userLimit == 0 || channel.userLimit > channel.members.size
        val hasPremiumUsers = isPremiumGuild(dao, channel.guild.idLong) || channel.members.any {
            dao.supporterWrapper.getUsers().contains(it.user.idLong)
        }
        val canMove = guild.selfMember.hasPermission(
            channel,
            Permission.VOICE_MOVE_OTHERS
        )

        return if (hasPremiumUsers && (channelNotFull || canMove)) {
            openConnection(channel, groupId)
            true
        } else {
            false
        }
    }

    private suspend fun hasPremiumUserElseMessage(
        context: ICommandContext,
        channel: VoiceChannel
    ): Boolean {
        val dao = context.daoManager
        val isPremium = isPremiumGuild(dao, channel.guild.idLong) || channel.members.any {
            dao.supporterWrapper.getUsers().contains(it.user.idLong)
        }
        if (!isPremium) sendRsp(context, i18n.getTranslation(context, "music.requires.premium")
            .replacePrefix(context))
        return isPremium
    }

    // run with VOICE_SAFE pls
    suspend fun closeConnection(guildId: Long, removeMusicPlayer: Boolean = true) {
        closeConnectionLite(guildId)

        if (removeMusicPlayer && MusicPlayerManager.guildMusicPlayers.containsKey(guildId)) {
            MusicPlayerManager.guildMusicPlayers.remove(guildId)?.removeTrackManagerListener()
        }
    }

    private suspend fun closeConnectionLite(guildId: Long) {
        val guild = MelijnBot.shardManager.getGuildById(guildId)

        if (jdaLavaLink == null) {
            guild?.audioManager?.closeAudioConnection()
        } else {
            jdaLavaLink.getExistingLink(guildId)?.destroy()
            logger.info(
                if (jdaLavaLink.getExistingLink(guildId) == null) {
                    "successfully destroyed $guildId"
                } else {
                    logger.warn("attempting force destroy")
                    jdaLavaLink.getExistingLink(guildId)?.forceDestroy()
                    if (jdaLavaLink.getExistingLink(guildId) == null) {
                        "successfully force destroyed $guildId"
                    } else {
                        "failed to destroy $guildId"
                    }
                }
            )
        }
    }

    fun getConnectedChannel(guild: Guild): VoiceChannel? = guild.selfMember.voiceState?.channel

    suspend fun changeGroup(guildId: Long, groupId: String) {
        val link = jdaLavaLink?.getLink(guildId, groupId) ?: throw IllegalArgumentException("wtf")
        link.changeGroup(groupId)
    }

    suspend fun closeConnectionForced(guildId: Long) {
        val guild = MelijnBot.shardManager.getGuildById(guildId)

        if (jdaLavaLink == null) {
            guild?.audioManager?.closeAudioConnection()

        } else {
            jdaLavaLink.getExistingLink(guildId)?.forceDestroy()
        }
    }
}