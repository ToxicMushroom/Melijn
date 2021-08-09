package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withSafeVarInCodeblock
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class SoftBanWrapper(private val softBanDao: SoftBanDao): PunishMapProvider<SoftBan> {

    fun addSoftBan(softBan: SoftBan) {
        softBanDao.addSoftBan(softBan)
    }

    override suspend fun getPunishMap(context: ICommandContext, targetUser: User): Map<Long, String> {
        val bans = softBanDao.getSoftBans(context.guildId, targetUser.idLong)
        return softSoftBansToMap(bans, context)
    }

    override suspend fun getPunishMap(context: ICommandContext, punishmentId: String): Map<Long, String> {
        val bans = softBanDao.getSoftBans(punishmentId)
        return softSoftBansToMap(bans, context)
    }

    private suspend fun softSoftBansToMap(softBans: List<SoftBan>, context: ICommandContext): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        if (softBans.isEmpty()) return emptyMap()
        softBans.forEach { ban ->
            val message = convertSoftBanInfoToMessage(context, ban)
            map[ban.moment] = message
        }
        return map
    }

    private suspend fun convertSoftBanInfoToMessage(context: ICommandContext, softBan: SoftBan): String {
        val kickAuthor = context.shardManager.retrieveUserById(softBan.softBanAuthorId).awaitOrNull()
        return getSoftBanMessage(context, kickAuthor, softBan)
    }

    private suspend fun getSoftBanMessage(context: ICommandContext, softBanAuthor: User?, softBan: SoftBan): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val zoneId = context.getTimeZoneId()
        return context.getTranslation("message.punishmenthistory.softban")
            .withSafeVarInCodeblock("softBanAuthor", softBanAuthor?.asTag ?: deletedUser)
            .withVariable("softBanAuthorId", "${softBan.softBanAuthorId}")
            .withSafeVarInCodeblock("reason", softBan.reason.substring(0, min(softBan.reason.length, 830)))
            .withVariable("moment", softBan.moment.asEpochMillisToDateTime(zoneId))
            .withVariable("softBanId", softBan.softBanId)

    }

    fun clear(guildId: Long, softBannedId: Long) {
        softBanDao.clear(guildId, softBannedId)
    }

    override suspend fun getPunishments(guildId: Long, targetId: Long): List<SoftBan> {
        return softBanDao.getSoftBans(guildId, targetId)
    }

    fun remove(softBan: SoftBan) {
        softBanDao.remove(softBan)
    }
}