package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class SoftBanWrapper(private val softBanDao: SoftBanDao) {

    suspend fun addSoftBan(softBan: SoftBan) {
        softBanDao.addSoftBan(softBan)
    }

    suspend fun getSoftBanMap(context: CommandContext, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val softBans = softBanDao.getSoftBans(context.guildId, targetUser.idLong)

        if (softBans.isEmpty()) {
            return emptyMap()
        }
        softBans.forEach { softBan ->
            val message = convertSoftBanInfoToMessage(context, softBan)
            map[softBan.moment] = message
        }
        return map
    }

    private suspend fun convertSoftBanInfoToMessage(context: CommandContext, softBan: SoftBan): String {
        val kickAuthor = context.shardManager.retrieveUserById(softBan.softBanAuthorId).awaitOrNull()
        return getSoftBanMessage(context, kickAuthor, softBan)
    }

    private suspend fun getSoftBanMessage(context: CommandContext, softBanAuthor: User?, softBan: SoftBan): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val zoneId = context.getTimeZoneId()
        return context.getTranslation("message.punishmenthistory.softban")
            .withSafeVariable("softBanAuthor", softBanAuthor?.asTag ?: deletedUser)
            .withVariable("softBanAuthorId", "${softBan.softBanAuthorId}")
            .withSafeVariable("reason", softBan.reason.substring(0, min(softBan.reason.length, 830)))
            .withVariable("moment", softBan.moment.asEpochMillisToDateTime(zoneId))
            .withVariable("softBanId", softBan.softBanId)

    }

    suspend fun getSoftBanMap(context: CommandContext, softbanId: String): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val softBans = softBanDao.getSoftBans(softbanId)
        if (softBans.isEmpty()) {
            return emptyMap()
        }

        softBans.forEach { softban ->
            val message = convertSoftBanInfoToMessage(context, softban)
            map[softban.moment] = message
        }

        return map
    }

    suspend fun clear(guildId: Long, softbannedId: Long) {
        softBanDao.clear(guildId, softbannedId)
    }

    suspend fun getSoftBans(guildId: Long, softbannedId: Long): List<SoftBan> {
        return softBanDao.getSoftBans(guildId, softbannedId)
    }

    suspend fun remove(softBan: SoftBan) {
        softBanDao.remove(softBan)
    }
}