package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.database.ban.PunishMapProvider
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.withSafeVarInCodeblock
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class KickWrapper(private val kickDao: KickDao): PunishMapProvider<Kick> {

    fun addKick(kick: Kick) {
        kickDao.add(kick)
    }

    override suspend fun getPunishMap(context: ICommandContext, targetUser: User): Map<Long, String> {
        val bans = kickDao.getKicks(context.guildId, targetUser.idLong)
        return kicksToMap(bans, context)
    }

    override suspend fun getPunishMap(context: ICommandContext, punishmentId: String): Map<Long, String> {
        val bans = kickDao.getKicks(punishmentId)
        return kicksToMap(bans, context)
    }

    private suspend fun kicksToMap(kicks: List<Kick>, context: ICommandContext): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        if (kicks.isEmpty()) return emptyMap()
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

    override suspend fun getPunishments(guildId: Long, targetId: Long): List<Kick> {
        return kickDao.getKicks(guildId, targetId)
    }

    fun remove(kick: Kick) {
        kickDao.remove(kick)
    }
}