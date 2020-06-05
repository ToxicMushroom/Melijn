package me.melijn.melijnbot.database.warn

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.awaitOrNull
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class WarnWrapper(val taskManager: TaskManager, private val warnDao: WarnDao) {

    suspend fun addWarn(warn: Warn) {
        warnDao.add(warn)
    }

    suspend fun getWarnMap(context: CommandContext, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val warns = warnDao.getWarns(context.guildId, targetUser.idLong)

        if (warns.isEmpty()) {
            return emptyMap()
        }

        warns.forEach { warn ->
            val message = convertWarnInfoToMessage(context, warn)
            map[warn.moment] = message
        }
        return map
    }

    private suspend fun convertWarnInfoToMessage(context: CommandContext, warn: Warn): String {
        val warnAuthor = context.shardManager.retrieveUserById(warn.warnAuthorId).awaitOrNull()
        return getWarnMessage(context, warnAuthor, warn)
    }

    private suspend fun getWarnMessage(context: CommandContext, warnAuthor: User?, warn: Warn): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val zoneId = context.getTimeZoneId()

        return context.getTranslation("message.punishmenthistory.warn")
            .replace("%warnAuthor%", warnAuthor?.asTag ?: deletedUser)
            .replace("%warnAuthorId%", "${warn.warnAuthorId}")
            .replace("%reason%", warn.reason.substring(0, min(warn.reason.length, 830)))
            .replace("%moment%", warn.moment.asEpochMillisToDateTime(zoneId))
            .replace("%warnId%", warn.warnId)
    }

    suspend fun getWarnMap(context: CommandContext, warnId: String): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val warns = warnDao.getWarns(warnId)
        if (warns.isEmpty()) {
            return emptyMap()
        }

        warns.forEach { warn ->
            val message = convertWarnInfoToMessage(context, warn)
            map[warn.moment] = message
        }

        return map
    }

    suspend fun clear(guildId: Long, warnedId: Long) {
        warnDao.clear(guildId, warnedId)
    }

    suspend fun getWarns(guildId: Long, warnedId: Long): List<Warn> {
        return warnDao.getWarns(guildId, warnedId)
    }

    suspend fun remove(warn: Warn) {
        warnDao.remove(warn)
    }
}