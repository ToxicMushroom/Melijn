package me.melijn.melijnbot.database.kick

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager

class KickWrapper(val taskManager: TaskManager, private val kickDao: KickDao) {

    fun addKick(kick: Kick) {
        kickDao.add(kick)
    }

    fun getKickMap(shardManager: ShardManager, guildId: Long, targetUser: User, timeKicks: (Map<Long, String>) -> Unit) {
        val map = hashMapOf<Long, String>()
        val kicks = kickDao.getKicks(guildId, targetUser.idLong)
        var counter = 0
        kicks.forEach { kick ->
            convertKickInfoToMessage(shardManager, kick) { message ->
                map[kick.kickMoment] = message
                if (++counter == kicks.size) {
                    timeKicks(map)
                }
            }
        }
    }

    private fun convertKickInfoToMessage(shardManager: ShardManager, kick: Kick, message: (String) -> Unit) {
        shardManager.retrieveUserById(kick.kickAuthorId).queue({ kickAuthor ->
            message(getKickMessage(kickAuthor, kick))
        }, {
            message(getKickMessage(null, kick))
        })

    }

    private fun getKickMessage(kickAuthor: User?, kick: Kick): String {
        return "```INI" +
                "\n[Kick Author] ${kickAuthor?.asTag ?: "deleted user"}" +
                "\n[Kick Author Id] ${kick.kickAuthorId}" +
                "\n[Kick Reason] ${kick.kickReason.substring(0, 830)}" +
                "\n[Moment] ${kick.kickMoment.asEpochMillisToDateTime()}}" +
                "```"

    }
}