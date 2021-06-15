package me.melijn.melijnbot.internals.services.bans

import me.melijn.melijnbot.commands.moderation.getUnbanMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.utils.awaitEX
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.getZoneId
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class BanService(
    val shardManager: ShardManager,
    val daoManager: DaoManager,
    val podInfo: PodInfo
) : Service("Ban", 1_000, 1_200, TimeUnit.MILLISECONDS) {

    override val service = RunnableTask {
        val bans = daoManager.banWrapper.getUnbannableBans(podInfo)
        for (ban in bans) {
            val selfUser = shardManager.shards[0].selfUser
            val newBan = ban.run {
                Ban(
                    guildId,
                    bannedId,
                    banAuthorId,
                    reason,
                    selfUser.idLong,
                    "Ban expired",
                    startTime,
                    endTime,
                    false,
                    banId
                )
            }
            daoManager.banWrapper.setBan(newBan)
            val guild = shardManager.getGuildById(ban.guildId) ?: continue

            //If ban exists, unban and send log messages
            val guildBan = guild.retrieveBanById(ban.bannedId).awaitOrNull() ?: continue
            val bannedUser = shardManager.retrieveUserById(guildBan.user.idLong).awaitOrNull() ?: continue
            val banAuthorId = ban.banAuthorId
            val banAuthor = if (banAuthorId == null) {
                null
            } else {
                shardManager.retrieveUserById(banAuthorId).awaitOrNull()
            }

            val exception = guild.unban(bannedUser).awaitEX()
            if (exception != null) {
                createAndSendFailedUnbanMessage(
                    guild, selfUser, bannedUser, banAuthor, newBan, exception.message
                        ?: ""
                )
                continue
            }

            createAndSendUnbanMessage(guild, selfUser, bannedUser, banAuthor, newBan)
        }
    }

    //Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnbanMessage(
        guild: Guild,
        unbanAuthor: User,
        bannedUser: User,
        banAuthor: User?,
        ban: Ban
    ) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNBAN) ?: return

        val msgLc = getUnbanMessage(
            language,
            zoneId,
            guild,
            bannedUser,
            banAuthor,
            unbanAuthor,
            ban,
            true,
            bannedUser.isBot,
            true
        )
        sendEmbed(daoManager.embedDisabledWrapper, logChannel, msgLc)
    }

    private suspend fun createAndSendFailedUnbanMessage(
        guild: Guild,
        unbanAuthor: User,
        bannedUser: User,
        banAuthor: User?,
        ban: Ban,
        cause: String
    ) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val privZoneId = getZoneId(daoManager, guild.idLong, bannedUser.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNBAN) ?: return

        val msgLc = getUnbanMessage(
            language,
            privZoneId,
            guild,
            bannedUser,
            banAuthor,
            unbanAuthor,
            ban,
            true,
            bannedUser.isBot,
            true,
            failedCause = cause
        )
        sendEmbed(daoManager.embedDisabledWrapper, logChannel, msgLc)
    }
}