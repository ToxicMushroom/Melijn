package me.melijn.melijnbot.objects.services.bans

import me.melijn.melijnbot.commands.moderation.getUnbanMessage
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.database.ban.BanWrapper
import me.melijn.melijnbot.database.embed.EmbedDisabledWrapper
import me.melijn.melijnbot.database.logchannels.LogChannelWrapper
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BanService(val shardManager: ShardManager,
                 private val banWrapper: BanWrapper,
                 private val logChannelWrapper: LogChannelWrapper,
                 private val embedDisabledWrapper: EmbedDisabledWrapper
) : Service("ban") {

    var scheduledFuture: ScheduledFuture<*>? = null

    private val banService = Runnable {
        val bans = banWrapper.getUnbannableBans()
        for (ban in bans) {
            val selfUser = shardManager.shards[0].selfUser
            val newBan = ban.run {
                Ban(guildId, bannedId, banAuthorId, reason, selfUser.idLong, "Ban expired", startTime, endTime, false)
            }
            banWrapper.setBan(newBan)
            val guild = shardManager.getGuildById(ban.guildId) ?: continue

            //If ban exists, unban and send log messages
            guild.retrieveBanById(ban.bannedId).queue({ guildBan ->
                shardManager.retrieveUserById(guildBan.user.idLong).queue({ bannedUser ->
                    guild.unban(bannedUser).queue()

                    shardManager.retrieveUserById(ban.banAuthorId ?: -1).queue({ banAuthor ->
                        createAndSendUnbanMessage(guild, selfUser, bannedUser, banAuthor, newBan)
                    }, {
                        createAndSendUnbanMessage(guild, selfUser, bannedUser, null, newBan)
                    })
                }, {})
            }, {})
        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private fun createAndSendUnbanMessage(guild: Guild, unbanAuthor: User, bannedUser: User, banAuthor: User?, ban: Ban) {
        val msg = getUnbanMessage(guild, bannedUser, banAuthor, unbanAuthor, ban)
        val channelId = logChannelWrapper.logChannelCache.get(Pair(guild.idLong, LogChannelType.TEMP_BAN)).get()
        val channel = guild.getTextChannelById(channelId)
        if (channel == null) {
            logChannelWrapper.removeChannel(guild.idLong, LogChannelType.TEMP_BAN)
            return
        }
        sendEmbed(embedDisabledWrapper, channel, msg)
        if (!bannedUser.isBot) {
            if (bannedUser.isFake) return
            bannedUser.openPrivateChannel().queue({ privateChannel ->
                sendEmbed(privateChannel, msg, failed = {})
            }, {})
        }
    }

    fun start() {
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(banService, 1_000, 1_000, TimeUnit.MILLISECONDS)
    }

    fun stop() {
        scheduledFuture?.cancel(false)
    }
}