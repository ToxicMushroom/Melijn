package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.await
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.math.min

class BanWrapper(val taskManager: TaskManager, private val banDao: BanDao) {

    suspend fun getUnbannableBans(): List<Ban> {
        return banDao.getUnbannableBans()
    }

    fun setBan(newBan: Ban) {
        banDao.setBan(newBan)
    }

    suspend fun getActiveBan(guildId: Long, bannedId: Long): Ban? {
        return banDao.getActiveBan(guildId, bannedId)
    }

    suspend fun getBanMap(shardManager: ShardManager, guildId: Long, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val bans = banDao.getBans(guildId, targetUser.idLong)
        if (bans.isEmpty()) {
            return emptyMap()
        }

        bans.forEach { ban ->
            val message = convertBanInfoToMessage(shardManager, ban)
            map[ban.startTime] = message
        }

        return map
    }

    private suspend fun convertBanInfoToMessage(shardManager: ShardManager, ban: Ban): String {
        val banAuthorId = ban.banAuthorId ?: return continueConvertingInfoToMessage(shardManager, null, ban)

        return try {
            val banAuthor = shardManager.retrieveUserById(banAuthorId).await()
            continueConvertingInfoToMessage(shardManager, banAuthor, ban)
        } catch (t: Throwable) {
            continueConvertingInfoToMessage(shardManager, null, ban)
        }
    }

    private suspend fun continueConvertingInfoToMessage(shardManager: ShardManager, banAuthor: User?, ban: Ban): String {
        val unbanAuthorId = ban.unbanAuthorId ?: return getBanMessage(banAuthor, null, ban)

        return try {
            val unbanAuthor = shardManager.retrieveUserById(unbanAuthorId).await()
            getBanMessage(banAuthor, unbanAuthor, ban)
        } catch (t: Throwable) {
            getBanMessage(banAuthor, null, ban)
        }
    }

    private fun getBanMessage(banAuthor: User?, unbanAuthor: User?, ban: Ban): String {
        val unbanReason = ban.unbanReason
        return "```INI" +
                "\n[Ban Author] ${banAuthor?.asTag ?: "deleted user"}" +
                "\n[Ban Author Id] ${ban.banAuthorId}" +
                "\n[Ban Reason] ${ban.reason.substring(0, min(ban.reason.length, 830))}" +
                "\n[Unban Reason] ${unbanReason?.substring(0, min(unbanReason.length, 830))}" +
                "\n[Unban Author] ${unbanAuthor?.asTag ?: "deleted user"}" +
                "\n[Start Time] ${ban.startTime.asEpochMillisToDateTime()}" +
                "\n[End Time] ${ban.endTime?.asEpochMillisToDateTime()}" +
                "\n[Active] ${ban.active}" +
                "```"

    }
}