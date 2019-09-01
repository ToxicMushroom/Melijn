package me.melijn.melijnbot.database.warn

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.await
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.math.min

class WarnWrapper(val taskManager: TaskManager, private val warnDao: WarnDao) {

    fun addWarn(warn: Warn) {
        warnDao.add(warn)
    }

    suspend fun getWarnMap(shardManager: ShardManager, guildId: Long, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val warns = warnDao.getWarns(guildId, targetUser.idLong)
        if (warns.isEmpty()) {
            return emptyMap()
        }

        warns.forEach { warn ->
            val message = convertKickInfoToMessage(shardManager, warn)
            map[warn.warnMoment] = message
        }
        return map
    }

    private suspend fun convertKickInfoToMessage(shardManager: ShardManager, warn: Warn): String {
        return try {
            val warnAuthor = shardManager.retrieveUserById(warn.warnAuthorId).await()
            getWarnMessage(warnAuthor, warn)
        } catch (t: Throwable) {
            getWarnMessage(null, warn)
        }
    }

    private fun getWarnMessage(warnAuthor: User?, warn: Warn): String {
        return "```INI" +
                "\n[Warn Author] ${warnAuthor?.asTag ?: "deleted user"}" +
                "\n[Warn Author Id] ${warn.warnAuthorId}" +
                "\n[Warn Reason] ${warn.warnReason.substring(0, min(warn.warnReason.length, 830))}" +
                "\n[Moment] ${warn.warnMoment.asEpochMillisToDateTime()}" +
                "```"

    }
}