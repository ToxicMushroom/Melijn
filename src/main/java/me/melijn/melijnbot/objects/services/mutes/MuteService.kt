package me.melijn.melijnbot.objects.services.mutes

import me.melijn.melijnbot.commands.moderation.getUnmuteMessage
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.database.logchannels.LogChannelWrapper
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.database.mute.MuteWrapper
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MuteService(val shardManager: ShardManager,
                  private val muteWrapper: MuteWrapper,
                  private val logChannelWrapper: LogChannelWrapper,
                  private val embedDisabledWrapper: EmbedDisabledWrapper) : Service("mute") {

    var scheduledFuture: ScheduledFuture<*>? = null

    private val muteService = Runnable {
        val mutes = muteWrapper.getUnmuteableMutes()
        for (mute in mutes) {
            val selfUser = shardManager.shards[0].selfUser
            val newMute = mute.run {
                Mute(guildId, mutedId, muteAuthorId, reason, selfUser.idLong, "Mute expired", startTime, endTime, false)
            }

            muteWrapper.setMute(newMute)
            val guild = shardManager.getGuildById(mute.guildId) ?: continue
            shardManager.retrieveUserById(newMute.muteAuthorId ?: -1).queue({ author ->
                shardManager.retrieveUserById(newMute.mutedId).queue({ muted ->
                    createAndSendUnmuteMessage(guild, selfUser, muted, author, newMute)
                }, {
                    createAndSendUnmuteMessage(guild, selfUser, null, author, newMute)
                })
            }, {
                shardManager.retrieveUserById(newMute.mutedId).queue({ muted ->
                    createAndSendUnmuteMessage(guild, selfUser, muted, null, newMute)
                }, {
                    createAndSendUnmuteMessage(guild, selfUser, null, null, newMute)
                })
            })

        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private fun createAndSendUnmuteMessage(guild: Guild, unbanAuthor: User, mutedUser: User?, muteAuthor: User?, mute: Mute) {
        val msg = getUnmuteMessage(guild, mutedUser, muteAuthor, unbanAuthor, mute)
        val channelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.UNMUTE)).get()
        val channel = guild.getTextChannelById(channelId)
        if (channel == null && channelId != -1L) {
            logChannelWrapper.removeChannel(guild.idLong, LogChannelType.UNMUTE)
            return
        } else if (channel == null) return

        sendEmbed(embedDisabledWrapper, channel, msg)

        if (mutedUser == null || mutedUser.isBot || mutedUser.isFake) return

        mutedUser.openPrivateChannel().queue({ privateChannel ->
            sendEmbed(privateChannel, msg, failed = {})
        }, {})
    }

    fun start() {
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(muteService, 1_000, 1_000, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        scheduledFuture?.cancel(false)
    }
}