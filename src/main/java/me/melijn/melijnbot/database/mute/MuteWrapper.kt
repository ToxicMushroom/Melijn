package me.melijn.melijnbot.database.mute

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import kotlin.math.min

class MuteWrapper(val taskManager: TaskManager, private val muteDao: MuteDao) {

    fun getUnmuteableMutes(): List<Mute> {
        return muteDao.getUnmuteableMutes()
    }

    fun setMute(newMute: Mute) {
        muteDao.setMute(newMute)
    }

    fun getActiveMute(guildId: Long, mutedId: Long): Mute? {
        return muteDao.getActiveMute(guildId, mutedId)
    }

    fun getMuteMap(shardManager: ShardManager, guildId: Long, targetUser: User, timeMutes: (Map<Long, String>) -> Unit) {
        val map = hashMapOf<Long, String>()
        val mutes = muteDao.getMutes(guildId, targetUser.idLong)
        var counter = 0
        if (mutes.isEmpty()) {
            timeMutes(emptyMap())
            return
        }
        mutes.forEach { ban ->
            convertMuteInfoToMessage(shardManager, ban) { message ->
                map[ban.startTime] = message
                if (++counter == mutes.size) {
                    timeMutes(map)
                }
            }
        }
    }

    private fun convertMuteInfoToMessage(shardManager: ShardManager, mute: Mute, message: (String) -> Unit) {
        val muteAuthorId = mute.muteAuthorId
        if (muteAuthorId == null) {
            continueConvertingInfoToMessage(shardManager, null, mute, message)
        } else {
            shardManager.retrieveUserById(muteAuthorId).queue({ muteAuthor ->
                continueConvertingInfoToMessage(shardManager, muteAuthor, mute, message)
            }, {
                continueConvertingInfoToMessage(shardManager, null, mute, message)
            })
        }

    }

    private fun continueConvertingInfoToMessage(shardManager: ShardManager, muteAuthor: User?, mute: Mute, message: (String) -> Unit) {
        val unbanAuthorId = mute.unmuteAuthorId
        if (unbanAuthorId == null) {
            message(getMuteMessage(muteAuthor, null, mute))
        } else {
            shardManager.retrieveUserById(unbanAuthorId).queue({ unmuteAuthor ->
                message(getMuteMessage(muteAuthor, unmuteAuthor, mute))
            }, {
                message(getMuteMessage(muteAuthor, null, mute))
            })
        }
    }

    private fun getMuteMessage(muteAuthor: User?, unmuteAuthor: User?, mute: Mute): String {
        val unmuteReason = mute.unmuteReason
        return "```INI" +
                "\n[Mute Author] ${muteAuthor?.asTag ?: "deleted user"}" +
                "\n[Mute Author Id] ${mute.muteAuthorId}" +
                "\n[Mute Reason] ${mute.reason.substring(0, min(mute.reason.length, 830))}" +
                "\n[Unmute Reason] ${unmuteReason?.substring(0, min(unmuteReason.length, 830))}" +
                "\n[Unmute Author] ${unmuteAuthor?.asTag ?: "deleted user"}" +
                "\n[Start Time] ${mute.startTime.asEpochMillisToDateTime()}" +
                "\n[End Time] ${mute.endTime?.asEpochMillisToDateTime()}" +
                "\n[Active] ${mute.active}```"

    }
}