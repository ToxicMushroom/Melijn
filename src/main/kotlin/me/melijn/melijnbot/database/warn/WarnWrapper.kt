package me.melijn.melijnbot.database.warn

import me.melijn.melijnbot.database.ban.PunishMapProvider
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withSafeVarInCodeblock
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class WarnWrapper(private val warnDao: WarnDao): PunishMapProvider<Warn> {

    fun addWarn(warn: Warn) {
        warnDao.add(warn)
    }

    override suspend fun getPunishMap(context: ICommandContext, targetUser: User): Map<Long, String> {
        val bans = warnDao.getWarns(context.guildId, targetUser.idLong)
        return warnsToMap(bans, context)
    }

    override suspend fun getPunishMap(context: ICommandContext, punishmentId: String): Map<Long, String> {
        val bans = warnDao.getWarns(punishmentId)
        return warnsToMap(bans, context)
    }

    private suspend fun warnsToMap(warns: List<Warn>, context: ICommandContext): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        if (warns.isEmpty()) return emptyMap()
        warns.forEach { ban ->
            val message = convertWarnInfoToMessage(context, ban)
            map[ban.moment] = message
        }
        return map
    }

    private suspend fun convertWarnInfoToMessage(context: ICommandContext, warn: Warn): String {
        val warnAuthor = context.shardManager.retrieveUserById(warn.warnAuthorId).awaitOrNull()
        return getWarnMessage(context, warnAuthor, warn)
    }

    private suspend fun getWarnMessage(context: ICommandContext, warnAuthor: User?, warn: Warn): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val zoneId = context.getTimeZoneId()

        return context.getTranslation("message.punishmenthistory.warn")
            .withSafeVarInCodeblock("warnAuthor", warnAuthor?.asTag ?: deletedUser)
            .withVariable("warnAuthorId", "${warn.warnAuthorId}")
            .withSafeVarInCodeblock("reason", warn.reason.substring(0, min(warn.reason.length, 830)))
            .withVariable("moment", warn.moment.asEpochMillisToDateTime(zoneId))
            .withVariable("warnId", warn.warnId)
    }

    fun clear(guildId: Long, warnedId: Long) {
        warnDao.clear(guildId, warnedId)
    }

    override suspend fun getPunishments(guildId: Long, targetId: Long): List<Warn> {
        return warnDao.getWarns(guildId, targetId)
    }

    fun remove(warn: Warn) {
        warnDao.remove(warn)
    }
}