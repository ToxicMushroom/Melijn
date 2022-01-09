package me.melijn.melijnbot.internals.services.bans

import io.ktor.client.*
import me.melijn.melijnbot.commands.moderation.getUnTempPunishMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.utils.awaitEX
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class BanService(
    val shardManager: ShardManager,
    val daoManager: DaoManager,
    val podInfo: PodInfo,
    val proxiedHttpClient: HttpClient
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

            // If ban exists, unban and send log messages
            val bannedUser = shardManager.retrieveUserById(ban.bannedId).awaitOrNull() ?: continue
            val banAuthorId = ban.banAuthorId
            val banAuthor = if (banAuthorId == null) null
            else shardManager.retrieveUserById(banAuthorId).awaitOrNull()

            val unbanError = guild.unban(bannedUser).reason("BanService: ban expired").awaitEX()
            createAndSendUnbanMessage(guild, selfUser, bannedUser, banAuthor, newBan, unbanError)
        }
    }

    // Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnbanMessage(
        guild: Guild,
        unbanAuthor: User,
        bannedUser: User,
        banAuthor: User?,
        ban: Ban,
        unbanError: Throwable?
    ) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNBAN) ?: return

        val msgLc = getUnTempPunishMessage(
            language,
            daoManager,
            guild,
            bannedUser,
            banAuthor,
            unbanAuthor,
            ban,
            lc = true,
            received = true,
            msgType = MessageType.UNBAN_LOG,
            failureCause = unbanError?.message
        )
        sendMsg(logChannel, proxiedHttpClient, msgLc)
    }
}