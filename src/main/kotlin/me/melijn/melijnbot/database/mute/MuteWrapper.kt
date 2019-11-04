package me.melijn.melijnbot.database.mute

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.awaitNE
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class MuteWrapper(val taskManager: TaskManager, private val muteDao: MuteDao) {

    fun getUnmuteableMutes(): List<Mute> {
        return muteDao.getUnmuteableMutes()
    }

    suspend fun setMute(newMute: Mute) {
        muteDao.setMute(newMute)
    }

    fun getActiveMute(guildId: Long, mutedId: Long): Mute? {
        return muteDao.getActiveMute(guildId, mutedId)
    }

    suspend fun getMuteMap(context: CommandContext, targetUser: User): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        val mutes = muteDao.getMutes(context.guildId, targetUser.idLong)
        if (mutes.isEmpty()) {
            return emptyMap()

        }
        mutes.forEach { ban ->
            val message = convertMuteInfoToMessage(context, ban)
            map[ban.startTime] = message
        }
        return map
    }

    private suspend fun convertMuteInfoToMessage(context: CommandContext, mute: Mute): String {
        val muteAuthorId = mute.muteAuthorId ?: return continueConvertingInfoToMessage(context, null, mute)

        val muteAuthor = context.shardManager.retrieveUserById(muteAuthorId).awaitNE()
        return continueConvertingInfoToMessage(context, muteAuthor, mute)
    }

    private suspend fun continueConvertingInfoToMessage(context: CommandContext, muteAuthor: User?, mute: Mute): String {
        val unbanAuthorId = mute.unmuteAuthorId ?: return getMuteMessage(context, muteAuthor, null, mute)

        val unmuteAuthor = context.shardManager.retrieveUserById(unbanAuthorId).awaitNE()
        return getMuteMessage(context, muteAuthor, unmuteAuthor, mute)
    }

    private suspend fun getMuteMessage(context: CommandContext, muteAuthor: User?, unmuteAuthor: User?, mute: Mute): String {
        val deletedUser = i18n.getTranslation(context, "message.deleted.user")
        val unmuteReason = mute.unmuteReason

        return i18n.getTranslation(context, "")
            .replace("%muteAuthor%", muteAuthor?.asTag ?: deletedUser)
            .replace("%muteAuthorId%", "${mute.muteAuthorId}")
            .replace("%unmuteAuthor%", if (mute.unmuteAuthorId == null) "/" else unmuteAuthor?.asTag ?: deletedUser)
            .replace("%unmuteAuthorId%", mute.unmuteAuthorId?.toString() ?: "/")
            .replace("%muteReason%", mute.reason.substring(0, min(mute.reason.length, 830)))
            .replace("%unmuteReason%", unmuteReason?.substring(0, min(unmuteReason.length, 830)) ?: "/")
            .replace("%startTime%", mute.startTime.asEpochMillisToDateTime())
            .replace("%endTime%", mute.endTime?.asEpochMillisToDateTime() ?: "/")
            .replace("%active%", "${mute.active}")
    }
}