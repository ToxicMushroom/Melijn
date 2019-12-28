package me.melijn.melijnbot.objects.services.bans

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.commands.moderation.getUnbanMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.awaitEX
import me.melijn.melijnbot.objects.utils.awaitOrNull
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BanService(
    val shardManager: ShardManager,
    val daoManager: DaoManager
) : Service("ban") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val banService = Runnable {
        runBlocking {
            val bans = daoManager.banWrapper.getUnbannableBans()
            for (ban in bans) {
                val selfUser = shardManager.shards[0].selfUser
                val newBan = ban.run {
                    Ban(guildId, bannedId, banAuthorId, reason, selfUser.idLong, "Ban expired", startTime, endTime, false)
                }
                daoManager.banWrapper.setBan(newBan)
                val guild = shardManager.getGuildById(ban.guildId) ?: continue

                //If ban exists, unban and send log messages
                val guildBan = guild.retrieveBanById(ban.bannedId).await()
                val bannedUser = shardManager.retrieveUserById(guildBan.user.idLong).awaitOrNull() ?: return@runBlocking
                val banAuthorId = ban.banAuthorId
                val banAuthor = if (banAuthorId == null) {
                    null
                } else {
                    shardManager.retrieveUserById(banAuthorId).awaitOrNull()
                }

                val exception = guild.unban(bannedUser).awaitEX()
                if (exception != null) {
                    createAndSendFailedUnbanMessage(guild, selfUser, bannedUser, banAuthor, newBan, exception.message
                        ?: "")
                    return@runBlocking
                }

                createAndSendUnbanMessage(guild, selfUser, bannedUser, banAuthor, newBan)
            }
        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnbanMessage(guild: Guild, unbanAuthor: User, bannedUser: User, banAuthor: User?, ban: Ban) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val msg = getUnbanMessage(language, guild, bannedUser, banAuthor, unbanAuthor, ban)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNBAN) ?: return

        var success = false
        if (!bannedUser.isBot) {

            val privateChannel = bannedUser.openPrivateChannel().awaitOrNull()
            if (privateChannel != null) {
                sendEmbed(privateChannel, msg)
                success = true
            }
        }

        val msgLc = getUnbanMessage(language, guild, bannedUser, banAuthor, unbanAuthor, ban, true, bannedUser.isBot, success)
        sendEmbed(daoManager.embedDisabledWrapper, logChannel, msgLc)
    }

    private suspend fun createAndSendFailedUnbanMessage(guild: Guild, unbanAuthor: User, bannedUser: User, banAuthor: User?, ban: Ban, cause: String) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val msg = getUnbanMessage(language, guild, bannedUser, banAuthor, unbanAuthor, ban, failedCause = cause)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNBAN) ?: return

        var success = false
        if (!bannedUser.isBot) {

            val privateChannel = bannedUser.openPrivateChannel().awaitOrNull()
            if (privateChannel != null) {
                sendEmbed(privateChannel, msg)
                success = true
            }
        }

        val msgLc = getUnbanMessage(language, guild, bannedUser, banAuthor, unbanAuthor, ban, true, bannedUser.isBot, success, failedCause = cause)
        sendEmbed(daoManager.embedDisabledWrapper, logChannel, msgLc)
    }

    override fun start() {
        logger.info("Started BanService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(banService, 1_000, 1_000, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        logger.info("Stopping BanService")
        scheduledFuture?.cancel(false)
    }
}