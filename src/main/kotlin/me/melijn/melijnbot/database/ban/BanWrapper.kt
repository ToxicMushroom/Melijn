package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.awaitOrNull
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class BanWrapper(val taskManager: TaskManager, private val banDao: BanDao) {

    suspend fun getUnbannableBans(): List<Ban> {
        return banDao.getUnbannableBans()
    }

    suspend fun setBan(newBan: Ban) {
        banDao.setBan(newBan)
    }

    suspend fun getActiveBan(guildId: Long, bannedId: Long): Ban? {
        return banDao.getActiveBan(guildId, bannedId)
    }

    suspend fun getBanMap(context: CommandContext, targetUser: User): Map<Long, String> {
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

    private suspend fun convertBanInfoToMessage(context: CommandContext, ban: Ban): String {
        val banAuthorId = ban.banAuthorId ?: return continueConvertingInfoToMessage(context, null, ban)
        val banAuthor = context.shardManager.retrieveUserById(banAuthorId).awaitOrNull()

        return continueConvertingInfoToMessage(context, banAuthor, ban)
    }

    private suspend fun continueConvertingInfoToMessage(context: CommandContext, banAuthor: User?, ban: Ban): String {
        val unbanAuthorId = ban.unbanAuthorId ?: return getBanMessage(context, banAuthor, null, ban)
        val unbanAuthor = context.shardManager.retrieveUserById(unbanAuthorId).awaitOrNull()

        return getBanMessage(context, banAuthor, unbanAuthor, ban)
    }

    private suspend fun getBanMessage(context: CommandContext, banAuthor: User?, unbanAuthor: User?, ban: Ban): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val unbanReason = ban.unbanReason
        val zoneId = context.getTimeZoneId()
        return context.getTranslation("message.punishmenthistory.ban")
            .replace("%banAuthor%", banAuthor?.asTag ?: deletedUser)
            .replace("%banAuthorId%", "${ban.banAuthorId}")
            .replace("%unbanAuthor%", if (ban.unbanAuthorId == null) "/" else unbanAuthor?.asTag ?: deletedUser)
            .replace("%unbanAuthorId%", ban.unbanAuthorId?.toString() ?: "/")
            .replace("%banReason%", ban.reason.substring(0, min(ban.reason.length, 830)))
            .replace("%unbanReason%", unbanReason?.substring(0, min(unbanReason.length, 830)) ?: "/")
            .replace("%startTime%", ban.startTime.asEpochMillisToDateTime(zoneId))
            .replace("%endTime%", ban.endTime?.asEpochMillisToDateTime(zoneId) ?: "/")
            .replace("%active%", "${ban.active}")
    }
}