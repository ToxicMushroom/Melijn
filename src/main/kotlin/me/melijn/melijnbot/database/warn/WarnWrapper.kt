package me.melijn.melijnbot.database.warn

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.translation.i18n
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
            val message = convertKickInfoToMessage(context, warn)
            map[warn.moment] = message
        }
        return map
    }

    private suspend fun convertKickInfoToMessage(context: CommandContext, warn: Warn): String {
        val warnAuthor = context.shardManager.retrieveUserById(warn.warnAuthorId).awaitOrNull()
        return getWarnMessage(context, warnAuthor, warn)
    }

    private suspend fun getWarnMessage(context: CommandContext, warnAuthor: User?, warn: Warn): String {
        val deletedUser = i18n.getTranslation(context, "message.deleted.user")
        return i18n.getTranslation(context, "message.punishmenthistory.warn")
            .replace("%warnAuthor%", warnAuthor?.asTag ?: deletedUser)
            .replace("%warnAuthorId%", "${warn.warnAuthorId}")
            .replace("%reason%", warn.reason.substring(0, min(warn.reason.length, 830)))
            .replace("%moment%", warn.moment.asEpochMillisToDateTime())
    }
}