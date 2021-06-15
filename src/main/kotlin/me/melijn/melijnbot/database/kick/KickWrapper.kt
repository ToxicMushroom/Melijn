package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withSafeVarInCodeblock
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class KickWrapper(private val kickDao: KickDao) {

    fun addKick(kick: Kick) {
        kickDao.add(kick)
    }

    suspend fun getKickMap(context: ICommandContext, targetUser: User): Map<Long, String> {
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

    suspend fun getKickMap(context: ICommandContext, kickId: String): Map<Long, String> {
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

    private suspend fun convertKickInfoToMessage(context: ICommandContext, kick: Kick): String {
        val kickAuthor = context.shardManager.retrieveUserById(kick.kickAuthorId).awaitOrNull()
        return getKickMessage(context, kickAuthor, kick)
    }

    private suspend fun getKickMessage(context: ICommandContext, kickAuthor: User?, kick: Kick): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val zoneId = context.getTimeZoneId()
        return context.getTranslation("message.punishmenthistory.kick")
            .withSafeVarInCodeblock("kickAuthor", kickAuthor?.asTag ?: deletedUser)
            .withVariable("kickAuthorId", "${kick.kickAuthorId}")
            .withSafeVarInCodeblock("reason", kick.reason.substring(0, min(kick.reason.length, 830)))
            .withVariable("moment", kick.moment.asEpochMillisToDateTime(zoneId))
            .withVariable("kickId", kick.kickId)
    }

    fun clear(guildId: Long, kickedId: Long) {
        kickDao.clear(guildId, kickedId)
    }

    suspend fun getKicks(guildId: Long, kickedId: Long): List<Kick> {
        return kickDao.getKicks(guildId, kickedId)
    }

    fun remove(kick: Kick) {
        kickDao.remove(kick)
    }
}