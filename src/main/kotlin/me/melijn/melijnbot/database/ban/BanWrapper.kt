package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class BanWrapper(private val banDao: BanDao) {

    suspend fun getUnbannableBans(podInfo: PodInfo): List<Ban> {
        return banDao.getUnbannableBans(podInfo)
    }

    fun setBan(newBan: Ban) {
        banDao.setBan(newBan)
    }

    suspend fun getActiveBan(guildId: Long, bannedId: Long): Ban? {
        return banDao.getActiveBan(guildId, bannedId)
    }

    fun clear(guildId: Long, bannedId: Long, clearActive: Boolean) {
        if (clearActive) {
            banDao.clearHistory(guildId, bannedId)
        } else {
            banDao.clear(guildId, bannedId)
        }
    }

    suspend fun getBans(guildId: Long, bannedId: Long): List<Ban> {
        return banDao.getBans(guildId, bannedId)
    }

    suspend fun getBanMap(context: ICommandContext, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val bans = banDao.getBans(context.guildId, targetUser.idLong)
        if (bans.isEmpty()) {
            return emptyMap()
        }

        bans.forEach { ban ->
            val message = convertBanInfoToMessage(context, ban)
            map[ban.startTime] = message
        }

        return map
    }

    suspend fun getBanMap(context: ICommandContext, banId: String): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val bans = banDao.getBans(banId)
        if (bans.isEmpty()) {
            return emptyMap()
        }

        bans.forEach { ban ->
            val message = convertBanInfoToMessage(context, ban)
            map[ban.startTime] = message
        }

        return map
    }

    private suspend fun convertBanInfoToMessage(context: ICommandContext, ban: Ban): String {
        val banAuthorId = ban.banAuthorId ?: return continueConvertingInfoToMessage(context, null, ban)
        val banAuthor = context.shardManager.retrieveUserById(banAuthorId).awaitOrNull()

        return continueConvertingInfoToMessage(context, banAuthor, ban)
    }

    private suspend fun continueConvertingInfoToMessage(context: ICommandContext, banAuthor: User?, ban: Ban): String {
        val unbanAuthorId = ban.unbanAuthorId ?: return getBanMessage(context, banAuthor, null, ban)
        val unbanAuthor = context.shardManager.retrieveUserById(unbanAuthorId).awaitOrNull()

        return getBanMessage(context, banAuthor, unbanAuthor, ban)
    }

    private suspend fun getBanMessage(
        context: ICommandContext,
        banAuthor: User?,
        unbanAuthor: User?,
        ban: Ban
    ): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val unbanReason = ban.unbanReason
        val zoneId = context.getTimeZoneId()

        val banDuration = ban.endTime?.let { endTime ->
            getDurationString((endTime - ban.startTime))
        } ?: context.getTranslation("infinite")

        return context.getTranslation("message.punishmenthistory.ban")
            .withSafeVarInCodeblock("banAuthor", banAuthor?.asTag ?: deletedUser)
            .withVariable("banAuthorId", "${ban.banAuthorId}")
            .withSafeVarInCodeblock(
                "unbanAuthor",
                if (ban.unbanAuthorId == null) "/" else unbanAuthor?.asTag ?: deletedUser
            )
            .withVariable("unbanAuthorId", ban.unbanAuthorId?.toString() ?: "/")
            .withSafeVarInCodeblock("banReason", ban.reason.substring(0, min(ban.reason.length, 830)))
            .withSafeVarInCodeblock("unbanReason", unbanReason?.substring(0, min(unbanReason.length, 830)) ?: "/")
            .withVariable("startTime", ban.startTime.asEpochMillisToDateTime(zoneId))
            .withVariable("endTime", ban.endTime?.asEpochMillisToDateTime(zoneId) ?: "/")
            .withVariable("duration", banDuration)
            .withVariable("banId", ban.banId)
            .withVariable("active", "${ban.active}")
    }

    suspend fun remove(ban: Ban) {
        banDao.remove(ban)
    }
}