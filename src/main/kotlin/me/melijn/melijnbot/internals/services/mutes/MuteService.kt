package me.melijn.melijnbot.internals.services.mutes

import me.melijn.melijnbot.commands.moderation.getUnmuteMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.utils.awaitEX
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.getZoneId
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class MuteService(
    val shardManager: ShardManager,
    val daoManager: DaoManager
) : Service("Mute", 1000, 1100, TimeUnit.MILLISECONDS) {

    override val service = RunnableTask {
        val mutes = daoManager.muteWrapper.getUnmuteableMutes()
        for (mute in mutes) {
            val selfUser = shardManager.shards[0].selfUser
            val newMute = mute.run {
                Mute(guildId, mutedId, muteAuthorId, reason, selfUser.idLong, "Mute expired", startTime, endTime, false, muteId)
            }

            daoManager.muteWrapper.setMute(newMute)
            val guild = shardManager.getGuildById(mute.guildId) ?: continue

            val muteRole = guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE, true)
            val author = shardManager.retrieveUserById(newMute.muteAuthorId ?: -1).awaitOrNull()
            val muted = shardManager.retrieveUserById(newMute.mutedId).awaitOrNull()
            val mutedMember = if (muted == null) null else guild.retrieveMember(muted).awaitOrNull()
            if (mutedMember != null && muteRole != null && mutedMember.roles.contains(muteRole)) {
                val exception = guild.removeRoleFromMember(mutedMember, muteRole).reason("unmuted").awaitEX()
                if (exception != null) {
                    createAndSendFailedUnmuteMessage(guild, selfUser, muted, author, newMute, exception.message
                        ?: "/")
                    continue
                }
            }

            createAndSendUnmuteMessage(guild, selfUser, muted, author, newMute)
        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnmuteMessage(guild: Guild, unmuteAuthor: User, mutedUser: User?, muteAuthor: User?, mute: Mute) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, mutedUser?.idLong)
        val msg = getUnmuteMessage(language, zoneId, guild, mutedUser, muteAuthor, unmuteAuthor, mute)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE) ?: return

        var success = false
        if (mutedUser?.isBot == false) {

            val privateChannel = mutedUser.openPrivateChannel().awaitOrNull()
            if (privateChannel != null) {
                sendEmbed(privateChannel, msg)
                success = true
            }
        }

        val msgLc = getUnmuteMessage(language, privZoneId, guild, mutedUser, muteAuthor, unmuteAuthor, mute, true, mutedUser?.isBot == true, success)
        sendEmbed(daoManager.embedDisabledWrapper, logChannel, msgLc)
    }

    private suspend fun createAndSendFailedUnmuteMessage(guild: Guild, unmuteAuthor: User, mutedUser: User?, muteAuthor: User?, mute: Mute, cause: String) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, mutedUser?.idLong)
        val msg = getUnmuteMessage(language, zoneId, guild, mutedUser, muteAuthor, unmuteAuthor, mute, failedCause = cause)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE) ?: return

        var success = false
        if (mutedUser?.isBot == false) {

            val privateChannel = mutedUser.openPrivateChannel().awaitOrNull()
            if (privateChannel != null) {
                sendEmbed(privateChannel, msg)
                success = true
            }
        }

        val msgLc = getUnmuteMessage(language, privZoneId, guild, mutedUser, muteAuthor, unmuteAuthor, mute, true, mutedUser?.isBot == true, success, failedCause = cause)
        sendEmbed(daoManager.embedDisabledWrapper, logChannel, msgLc)
    }
}