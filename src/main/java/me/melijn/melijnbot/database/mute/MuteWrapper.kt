package me.melijn.melijnbot.database.mute

import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.await
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

    suspend fun getMuteMap(shardManager: ShardManager, guildId: Long, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val mutes = muteDao.getMutes(guildId, targetUser.idLong)
        if (mutes.isEmpty()) {
            return emptyMap()

        }
        mutes.forEach { ban ->
            val message = convertMuteInfoToMessage(shardManager, ban)
            map[ban.startTime] = message
        }
        return map
    }

    private suspend fun convertMuteInfoToMessage(shardManager: ShardManager, mute: Mute): String {
        val muteAuthorId = mute.muteAuthorId ?: return continueConvertingInfoToMessage(shardManager, null, mute)
        return try {
            val muteAuthor = shardManager.retrieveUserById(muteAuthorId).await()
            continueConvertingInfoToMessage(shardManager, muteAuthor, mute)
        } catch (t: Throwable) {
            continueConvertingInfoToMessage(shardManager, null, mute)
        }
    }

    private suspend fun continueConvertingInfoToMessage(shardManager: ShardManager, muteAuthor: User?, mute: Mute): String {
        val unbanAuthorId = mute.unmuteAuthorId ?: return getMuteMessage(muteAuthor, null, mute)
        return try {
            val unmuteAuthor = shardManager.retrieveUserById(unbanAuthorId).await()
            getMuteMessage(muteAuthor, unmuteAuthor, mute)
        } catch (t: Throwable) {
            getMuteMessage(muteAuthor, null, mute)
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