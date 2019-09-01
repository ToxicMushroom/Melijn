package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.await
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.math.min

class KickWrapper(val taskManager: TaskManager, private val kickDao: KickDao) {

    fun addKick(kick: Kick) {
        kickDao.add(kick)
    }

    suspend fun getKickMap(shardManager: ShardManager, guildId: Long, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val kicks = kickDao.getKicks(guildId, targetUser.idLong)

        if (kicks.isEmpty()) {
            return emptyMap()
        }
        kicks.forEach { kick ->
            val message = convertKickInfoToMessage(shardManager, kick)
            map[kick.kickMoment] = message
        }
        return map
    }

    private suspend fun convertKickInfoToMessage(shardManager: ShardManager, kick: Kick): String {
        return try {
            val kickAuthor = shardManager.retrieveUserById(kick.kickAuthorId).await()
            getKickMessage(kickAuthor, kick)
        } catch (t: Throwable) {
            getKickMessage(null, kick)
        }
    }

    private fun getKickMessage(kickAuthor: User?, kick: Kick): String {
        return "```INI" +
                "\n[Kick Author] ${kickAuthor?.asTag ?: "deleted user"}" +
                "\n[Kick Author Id] ${kick.kickAuthorId}" +
                "\n[Kick Reason] ${kick.kickReason.substring(0, min(kick.kickReason.length, 830))}" +
                "\n[Moment] ${kick.kickMoment.asEpochMillisToDateTime()}" +
                "```"

    }
}