package me.melijn.melijnbot.internals.events.eventutil

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.music.NextSongPosition
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelRoleState
import me.melijn.melijnbot.internals.music.LavaManager
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.awaitBool
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyMusicChannel
import me.melijn.melijnbot.internals.utils.isPremiumGuild
import me.melijn.melijnbot.internals.utils.listeningMembers
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.StatusChangeEvent
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture

object VoiceUtil {

    //guildId, timeOfLeave
    var disconnectQueue = ConcurrentHashMap<Long, ScheduledFuture<*>>()

    suspend fun channelUpdate(container: Container, channelUpdate: VoiceChannel) {
        val guild = channelUpdate.guild
        val botChannel = container.lavaManager.getConnectedChannel(guild)
        val daoManager = container.daoManager

        // Leave channel timer stuff
        botChannel?.let {
            checkShouldDisconnectAndApply(it, container.lavaManager, daoManager)
        }

        // Radio stuff
        val musicChannel = guild.getAndVerifyMusicChannel(daoManager, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
            ?: return

        val musicUrl = daoManager.streamUrlWrapper.getUrl(guild.idLong)
        if (musicUrl.isBlank()) return

        val musicPlayerManager = container.lavaManager.musicPlayerManager
        val trackManager = musicPlayerManager.getGuildMusicPlayer(guild).guildTrackManager
        val audioLoader = musicPlayerManager.audioLoader
        val iPlayer = trackManager.iPlayer

        if (musicChannel.id == botChannel?.id && channelUpdate.id == botChannel.id && iPlayer.playingTrack != null) {
            return
        } else if (musicChannel.id == botChannel?.id && channelUpdate.id == botChannel.id) {
            audioLoader.loadNewTrack(
                daoManager,
                container.lavaManager,
                channelUpdate,
                guild.jda.selfUser,
                musicUrl,
                NextSongPosition.BOTTOM
            )

        } else if (botChannel == null && musicChannel.id == channelUpdate.id) {
            if (listeningMembers(musicChannel, container.settings.botInfo.id) > 0) {
                val groupId = trackManager.groupId
                if (container.lavaManager.tryToConnectToVCSilent(musicChannel, groupId)) {
                    audioLoader.loadNewTrack(
                        daoManager,
                        container.lavaManager,
                        channelUpdate,
                        guild.jda.selfUser,
                        musicUrl,
                        NextSongPosition.BOTTOM
                    )
                }
            }
        }
    }

    suspend fun checkShouldDisconnectAndApply(botChannel: VoiceChannel, lavaManager: LavaManager, daoManager: DaoManager) {
        val guildId = botChannel.guild.idLong
        if (
            listeningMembers(botChannel) == 0 &&
            !(daoManager.music247Wrapper.is247Mode(guildId) && isPremiumGuild(daoManager, guildId))
        ) {
            val job = TaskManager.asyncAfter(600_000) {
                val guildMusicPlayer = lavaManager.musicPlayerManager.getGuildMusicPlayer(botChannel.guild)
                guildMusicPlayer.guildTrackManager.clear()
                guildMusicPlayer.guildTrackManager.stopAndDestroy()
            }
            disconnectQueue[guildId] = job
        } else {
            disconnectQueue.remove(guildId)?.cancel(false)
        }
    }

    fun getConnectedChannelsAmount(shardManager: ShardManager, andHasListeners: Boolean = false): Long {
        return shardManager.shards.stream().mapToLong { shard ->
            getConnectedChannelsAmount(shard, andHasListeners)
        }?.sum() ?: 0
    }

    fun getConnectedChannelsAmount(inShard: JDA, andHasListeners: Boolean = false): Long {
        return inShard.voiceChannels.stream().filter { vc ->
            val contains = vc.members.contains(vc.guild.selfMember)
            val lm = listeningMembers(vc)
            if (andHasListeners) {
                contains && lm > 0
            } else {
                contains
            }
        }.count()
    }

    suspend fun resumeMusic(event: StatusChangeEvent, container: Container) {
        val wrapper = container.daoManager.tracksWrapper
        val music = wrapper.getMap()
        val channelMap = wrapper.getChannels()
        val shardManager = event.jda.shardManager ?: return
        val mpm = container.lavaManager.musicPlayerManager
        for ((guildId, tracks) in music) {
            val guild = shardManager.getGuildById(guildId) ?: continue
            val channel = channelMap[guildId]?.let { guild.getVoiceChannelById(it) } ?: continue

            val groupId = container.lavaManager.musicPlayerManager.getGuildMusicPlayer(guild).groupId
            if (container.lavaManager.tryToConnectToVCSilent(channel, groupId)) {
                val mp = mpm.getGuildMusicPlayer(guild)
                for (track in tracks) {
                    mp.safeQueueSilent(container.daoManager, track, NextSongPosition.BOTTOM)
                }
            }
        }
        wrapper.clearChannels()
        wrapper.clear()
    }

    suspend fun destroyLink(container: Container, member: Member) {
        container.lavaManager.closeConnection(member.guild.idLong)
    }

    suspend fun handleChannelRoleMove(
        daoManager: DaoManager,
        member: Member,
        channelJoined: VoiceChannel?,
        channelLeft: VoiceChannel?
    ) {
        val guild = member.guild
        val selfMember = guild.selfMember
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return

        val wrapper = daoManager.channelRoleWrapper
        val mapChannel1 = channelLeft?.let { wrapper.getRoleIds(guild.idLong, it.idLong) } ?: emptyMap()
        val mapChannel2 = channelJoined?.let { wrapper.getRoleIds(guild.idLong, it.idLong) } ?: emptyMap()
        val softGrant1 = mapChannel1[ChannelRoleState.SOFT_GRANT]?.toMutableList() ?: mutableListOf()
        val softTake1 = mapChannel1[ChannelRoleState.SOFT_TAKE]?.toMutableList() ?: mutableListOf()

        val softGrant2 = mapChannel2[ChannelRoleState.SOFT_GRANT]?.toMutableList() ?: mutableListOf()
        val softTake2 = mapChannel2[ChannelRoleState.SOFT_TAKE]?.toMutableList() ?: mutableListOf()

        val mutualSoftGrant = softGrant1.filter { softGrant2.contains(it) }
        val mutualSoftTake = softTake1.filter { softTake2.contains(it) }

        softGrant1.removeAll(mutualSoftGrant) // if you leave vc 1 where you got role x granted and move to vc 2 with role x granted, we wont try to remove the role between channels
        softTake1.removeAll(mutualSoftTake) // same but for taking roles

        if (softGrant1.isNotEmpty()
            || softTake1.isNotEmpty()
            || member.roles.any { softGrant2.contains(it.idLong) }
            || member.roles.none { softTake2.contains(it.idLong) }
        ) {
            val userRoles = daoManager.userChannelRoleWrapper.get(member.idLong)
            val userSoftTaken = userRoles[ChannelRoleState.SOFT_TAKE]
                ?: emptyList() // User already didnt have the roles in this list before soft_take
            val userSoftGranted = userRoles[ChannelRoleState.SOFT_GRANT]
                ?: emptyList() // User already had the roles in this list before soft_grant

            softGrant1.removeAll(userSoftGranted) // softgrant now contains only the roles that were granted and need to be taken
            softTake1.removeAll(userSoftTaken) // softtake now contains only the roles that were taken and need to be granted

            if (userSoftTaken != softTake2 || userSoftGranted != softGrant2) {
                if (userSoftTaken.isNotEmpty() || userSoftGranted.isNotEmpty())
                    daoManager.userChannelRoleWrapper.clear(member.idLong)

                // this map contains the soft_granted or taken roles which means the user already had the granted role or already didnt have the taken role and
                // after leaving voice these soft_granted roles should not be taken away, same stuff for soft_taken
                val softMap = mapChannel2.filter {
                    it.key == ChannelRoleState.SOFT_GRANT || it.key == ChannelRoleState.SOFT_TAKE
                }.map {
                    it.key to it.value.filter { roleId ->
                        val predicate: (Role) -> Boolean = { role -> role.idLong == roleId }

                        if (it.key == ChannelRoleState.SOFT_GRANT) member.roles.any(predicate)
                        else member.roles.none(predicate)
                    }
                }.toMap()

                daoManager.userChannelRoleWrapper.setBulk(member.idLong, softMap)
            }
        }

        removeRoles(member, softGrant1)
        addRoles(member, softTake1)

        removeRoles(member, softTake2)
        addRoles(member, softGrant2)

        for ((state, roles) in mapChannel1) {
            // channel 2 does not have this state_role
            val predicate: (Long) -> Boolean = { mapChannel2[state]?.contains(it) != true }
            when (state) {
                // granted in channel 1 and went over predicate, so now remove
                ChannelRoleState.GRANT -> removeRoles(member, roles.filter(predicate))
                // taken in channel 1 and went over predicate, so now grant
                ChannelRoleState.TAKE -> addRoles(member, roles.filter(predicate))
                else -> Unit
            }
        }

        for ((state, roles) in mapChannel2) {
            when (state) {
                ChannelRoleState.GRANT -> addRoles(member, roles)
                ChannelRoleState.TAKE -> removeRoles(member, roles)
                else -> Unit
            }
        }

        for ((state, roles) in mapChannel2) {
            when (state) {
                ChannelRoleState.HARD_GRANT -> addRoles(member, roles)
                ChannelRoleState.HARD_TAKE -> removeRoles(member, roles)
                else -> Unit
            }
        }
    }

    suspend fun handleChannelRoleJoin(daoManager: DaoManager, member: Member, channelJoined: VoiceChannel) {
        val guild = channelJoined.guild
        val selfMember = guild.selfMember
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return

        handleChannelRoleMove(daoManager, member, channelJoined, null)
    }

    suspend fun handleChannelRoleLeave(daoManager: DaoManager, member: Member, channelLeft: VoiceChannel) {
        val guild = channelLeft.guild
        val selfMember = guild.selfMember
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return

        handleChannelRoleMove(daoManager, member, null, channelLeft)
    }

    private suspend fun removeRoles(
        member: Member,
        roles: List<Long>
    ): List<Long> {
        val guild = member.guild

        return roles.filter { roleId ->
            val role = guild.getRoleById(roleId) ?: return@filter false

            !guild.selfMember.canInteract(role) || (!guild.removeRoleFromMember(member, role).reason("channelRole")
                .awaitBool())
        }
    }

    /**
     * tries adding roles in [roles] to [member] and returns a list of roles that failed to be added
     */
    private suspend fun addRoles(
        member: Member,
        roles: List<Long>
    ): List<Long> {
        val guild = member.guild

        return roles.filter { roleId ->
            val role = guild.getRoleById(roleId) ?: return@filter false

            !guild.selfMember.canInteract(role) || (!guild.addRoleToMember(member, role).reason("channelRole")
                .awaitBool())
        }
    }
}