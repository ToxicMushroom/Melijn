package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.awaitOrNull
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class KickWrapper(val taskManager: TaskManager, private val kickDao: KickDao) {

    suspend fun addKick(kick: Kick) {
        kickDao.add(kick)
    }

    suspend fun getKickMap(context: CommandContext, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val kicks = kickDao.getKicks(context.guildId, targetUser.idLong)

        if (kicks.isEmpty()) {
            return emptyMap()
        }
        kicks.forEach { kick ->
            val message = convertKickInfoToMessage(context, kick)
            map[kick.moment] = message
        }
        return map
    }

    suspend fun getKickMap(context: CommandContext, kickId: String): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val kicks = kickDao.getKicks(kickId)
        if (kicks.isEmpty()) {
            return emptyMap()
        }

        kicks.forEach { kick ->
            val message = convertKickInfoToMessage(context, kick)
            map[kick.moment] = message
        }

        return map
    }

    private suspend fun convertKickInfoToMessage(context: CommandContext, kick: Kick): String {
        val kickAuthor = context.shardManager.retrieveUserById(kick.kickAuthorId).awaitOrNull()
        return getKickMessage(context, kickAuthor, kick)
    }

    private suspend fun getKickMessage(context: CommandContext, kickAuthor: User?, kick: Kick): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val zoneId = context.getTimeZoneId()
        return context.getTranslation("message.punishmenthistory.kick")
            .replace("%kickAuthor%", kickAuthor?.asTag ?: deletedUser)
            .replace("%kickAuthorId%", "${kick.kickAuthorId}")
            .replace("%reason%", kick.reason.substring(0, min(kick.reason.length, 830)))
            .replace("%moment%", kick.moment.asEpochMillisToDateTime(zoneId))
            .replace("%kickId%", kick.kickId)
    }

    suspend fun clear(guildId: Long, kickedId: Long) {
        kickDao.clear(guildId, kickedId)
    }

    suspend fun getKicks(guildId: Long, kickedId: Long): List<Kick> {
        return kickDao.getKicks(guildId, kickedId)
    }

    suspend fun remove(kick: Kick) {
        kickDao.remove(kick)
    }
}