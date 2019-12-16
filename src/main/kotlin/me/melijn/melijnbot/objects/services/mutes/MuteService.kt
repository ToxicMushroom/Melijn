package me.melijn.melijnbot.objects.services.mutes

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.commands.moderation.getUnmuteMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.database.mute.MuteWrapper
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.utils.awaitEX
import me.melijn.melijnbot.objects.utils.awaitOrNull
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MuteService(
    val shardManager: ShardManager,
    private val muteWrapper: MuteWrapper,
    private val embedDisabledWrapper: EmbedDisabledWrapper,
    val daoManager: DaoManager
) : Service("mute") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val muteService = Runnable {
        runBlocking {
            val mutes = muteWrapper.getUnmuteableMutes()
            for (mute in mutes) {
                val selfUser = shardManager.shards[0].selfUser
                val newMute = mute.run {
                    Mute(guildId, mutedId, muteAuthorId, reason, selfUser.idLong, "Mute expired", startTime, endTime, false)
                }

                muteWrapper.setMute(newMute)
                val guild = shardManager.getGuildById(mute.guildId) ?: continue

                val muteRole = guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE, true)
                val author = shardManager.retrieveUserById(newMute.muteAuthorId ?: -1).awaitOrNull()
                val muted = shardManager.retrieveUserById(newMute.mutedId).awaitOrNull()
                val mutedMember = if (muted == null) null else guild.getMember(muted)
                if (mutedMember != null && muteRole != null && mutedMember.roles.contains(muteRole)) {
                    val exception = guild.removeRoleFromMember(mutedMember, muteRole).awaitEX()
                    if (exception != null) {
                        createAndSendFailedUnmuteMessage(guild, selfUser, muted, author, newMute, exception.message
                            ?: "/")
                        return@runBlocking
                    }
                }

                createAndSendUnmuteMessage(guild, selfUser, muted, author, newMute)
            }
        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnmuteMessage(guild: Guild, unmuteAuthor: User, mutedUser: User?, muteAuthor: User?, mute: Mute) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val msg = getUnmuteMessage(language, guild, mutedUser, muteAuthor, unmuteAuthor, mute)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE) ?: return

        var success = false
        if (mutedUser?.isBot == false) {
            //if (mutedUser?.isFake == true) return

            val privateChannel = mutedUser.openPrivateChannel().awaitOrNull()
            if (privateChannel != null) {
                sendEmbed(privateChannel, msg)
                success = true
            }
        }

        val msgLc = getUnmuteMessage(language, guild, mutedUser, muteAuthor, unmuteAuthor, mute, true, mutedUser?.isBot == true, success)
        sendEmbed(embedDisabledWrapper, logChannel, msgLc)
    }

    private suspend fun createAndSendFailedUnmuteMessage(guild: Guild, unmuteAuthor: User, mutedUser: User?, muteAuthor: User?, mute: Mute, cause: String) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val msg = getUnmuteMessage(language, guild, mutedUser, muteAuthor, unmuteAuthor, mute, failedCause = cause)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE) ?: return

        var success = false
        if (mutedUser?.isBot == false) {
            //if (mutedUser?.isFake == true) return

            val privateChannel = mutedUser.openPrivateChannel().awaitOrNull()
            if (privateChannel != null) {
                sendEmbed(privateChannel, msg)
                success = true
            }
        }

        val msgLc = getUnmuteMessage(language, guild, mutedUser, muteAuthor, unmuteAuthor, mute, true, mutedUser?.isBot == true, success, failedCause = cause)
        sendEmbed(embedDisabledWrapper, logChannel, msgLc)
    }

    override fun start() {
        logger.info("Started MuteService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(muteService, 1_100, 1_000, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        logger.info("Stopping MuteService")
        scheduledFuture?.cancel(false)
    }
}