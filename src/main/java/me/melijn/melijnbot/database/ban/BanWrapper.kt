package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.math.min

class BanWrapper(val taskManager: TaskManager, private val banDao: BanDao) {

    fun getUnbannableBans(): List<Ban> {
        return banDao.getUnbannableBans()
    }

    fun setBan(newBan: Ban) {
        banDao.setBan(newBan)
    }

    fun getActiveBan(guildId: Long, bannedId: Long): Ban? {
        return banDao.getActiveBan(guildId, bannedId)
    }

    fun getBanMap(shardManager: ShardManager, guildId: Long, targetUser: User, timeBans: (Map<Long, String>) -> Unit) {
        val map = hashMapOf<Long, String>()
        val bans = banDao.getBans(guildId, targetUser.idLong)
        var counter = 0
        if (bans.isEmpty()) {
            timeBans(emptyMap())
            return
        }
        bans.forEach { ban ->
            convertBanInfoToMessage(shardManager, ban) { message ->
                map[ban.startTime] = message
                if (++counter == bans.size) {
                    timeBans(map)
                }
            }
        }
    }

    private fun convertBanInfoToMessage(shardManager: ShardManager, ban: Ban, message: (String) -> Unit) {
        val banAuthorId = ban.banAuthorId
        if (banAuthorId == null) {
            continueConvertingInfoToMessage(shardManager, null, ban, message)
        } else {
            shardManager.retrieveUserById(banAuthorId).queue({ banAuthor ->
                continueConvertingInfoToMessage(shardManager, banAuthor, ban, message)
            }, {
                continueConvertingInfoToMessage(shardManager, null, ban, message)
            })
        }

    }

    private fun continueConvertingInfoToMessage(shardManager: ShardManager, banAuthor: User?, ban: Ban, message: (String) -> Unit) {
        val unbanAuthorId = ban.unbanAuthorId
        if (unbanAuthorId == null) {
            message(getBanMessage(banAuthor, null, ban))
        } else {
            shardManager.retrieveUserById(unbanAuthorId).queue({ unbanAuthor ->
                message(getBanMessage(banAuthor, unbanAuthor, ban))
            }, {
                message(getBanMessage(banAuthor, null, ban))
            })
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