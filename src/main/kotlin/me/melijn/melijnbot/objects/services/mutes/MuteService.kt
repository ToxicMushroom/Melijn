package me.melijn.melijnbot.objects.services.mutes

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.commands.moderation.getUnmuteMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.database.logchannel.LogChannelWrapper
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.database.mute.MuteWrapper
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MuteService(
    val shardManager: ShardManager,
    private val muteWrapper: MuteWrapper,
    private val logChannelWrapper: LogChannelWrapper,
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
                try {
                    val author = shardManager.retrieveUserById(newMute.muteAuthorId ?: -1).await()
                    try {
                        val muted = shardManager.retrieveUserById(newMute.mutedId).await()
                        createAndSendUnmuteMessage(guild, selfUser, muted, author, newMute)
                    } catch (t: Throwable) {
                        createAndSendUnmuteMessage(guild, selfUser, null, author, newMute)
                    }
                } catch (t: Throwable) {
                    try {
                        val muted = shardManager.retrieveUserById(newMute.mutedId).await()
                        createAndSendUnmuteMessage(guild, selfUser, muted, null, newMute)
                    } catch (t: Throwable) {
                        createAndSendUnmuteMessage(guild, selfUser, null, null, newMute)
                    }
                }
            }
        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnmuteMessage(guild: Guild, unmuteAuthor: User, mutedUser: User?, muteAuthor: User?, mute: Mute) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val msg = getUnmuteMessage(language, guild, mutedUser, muteAuthor, unmuteAuthor, mute)
        val channelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.UNMUTE)).await()
        val channel = guild.getTextChannelById(channelId)

        var success = false
        if (mutedUser?.isBot != true) {
            if (mutedUser?.isFake == true) return
            try {
                val privateChannel = mutedUser?.openPrivateChannel()?.await()
                privateChannel?.let {
                    sendEmbed(it, msg)
                }

                success = true
            } catch (t: Throwable) {
            }
        }

        val msgLc = getUnmuteMessage(language, guild, mutedUser, muteAuthor, unmuteAuthor, mute, true, mutedUser?.isBot == true, success)

        if (channel == null && channelId != -1L) {
            logChannelWrapper.removeChannel(guild.idLong, LogChannelType.UNMUTE)
            return
        } else if (channel == null) return

        sendEmbed(embedDisabledWrapper, channel, msgLc)
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