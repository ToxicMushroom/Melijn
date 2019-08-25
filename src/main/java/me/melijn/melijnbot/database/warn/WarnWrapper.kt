package me.melijn.melijnbot.database.warn

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager

class WarnWrapper(val taskManager: TaskManager, private val warnDao: WarnDao) {

    fun addWarn(warn: Warn) {
        warnDao.add(warn)
    }

    fun getWarnMap(shardManager: ShardManager, guildId: Long, targetUser: User, timeWarns: (Map<Long, String>) -> Unit) {
        val map = hashMapOf<Long, String>()
        val warns = warnDao.getWarns(guildId, targetUser.idLong)
        var counter = 0
        warns.forEach { warn ->
            convertKickInfoToMessage(shardManager, warn) { message ->
                map[warn.warnMoment] = message
                if (++counter == warns.size) {
                    timeWarns(map)
                }
            }
        }
    }

    private fun convertKickInfoToMessage(shardManager: ShardManager, warn: Warn, message: (String) -> Unit) {
        shardManager.retrieveUserById(warn.warnAuthorId).queue({ warnAuthor ->
            message(getWarnMessage(warnAuthor, warn))
        }, {
            message(getWarnMessage(null, warn))
        })

    }

    private fun getWarnMessage(warnAuthor: User?, warn: Warn): String {
        return "```INI" +
                "\n[Warn Author] ${warnAuthor?.asTag ?: "deleted user"}" +
                "\n[Warn Author Id] ${warn.warnAuthorId}" +
                "\n[Warn Reason] ${warn.warnReason.substring(0, 830)}" +
                "\n[Moment] ${warn.warnMoment.asEpochMillisToDateTime()}}" +
                "```"

    }
}