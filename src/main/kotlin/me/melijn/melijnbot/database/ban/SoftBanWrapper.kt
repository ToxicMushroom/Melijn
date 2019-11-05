package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.awaitOrNull
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class SoftBanWrapper(val taskManager: TaskManager, private val softBanDao: SoftBanDao) {

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
        val deletedUser = i18n.getTranslation(context, "message.deleted.user")
        return i18n.getTranslation(context, "message.punishmenthistory.softban")
            .replace("%softBanAuthor%", softBanAuthor?.asTag ?: deletedUser)
            .replace("%softBanAuthorId%", "${softBan.softBanAuthorId}")
            .replace("%reason%", softBan.reason.substring(0, min(softBan.reason.length, 830)))
            .replace("%moment%", softBan.moment.asEpochMillisToDateTime())
    }
}