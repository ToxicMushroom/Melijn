package me.melijn.melijnbot.database.mute

import me.melijn.melijnbot.database.ban.PunishMapProvider
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.entities.User
import kotlin.math.min

class MuteWrapper(private val muteDao: MuteDao): PunishMapProvider<Mute> {

    suspend fun getUnmuteableMutes(podInfo: PodInfo): List<Mute> {
        return muteDao.getUnmuteableMutes(podInfo)
    }

    fun setMute(newMute: Mute) {
        muteDao.setMute(newMute)
    }

    fun getActiveMute(guildId: Long, mutedId: Long): Mute? {
        return muteDao.getActiveMute(guildId, mutedId)
    }

    override suspend fun getPunishMap(context: ICommandContext, targetUser: User): Map<Long, String> {
        val mutes = muteDao.getMutes(context.guildId, targetUser.idLong)
        return mutesToMap(mutes, context)
    }

    override suspend fun getPunishMap(context: ICommandContext, punishmentId: String): Map<Long, String> {
        val mutes = muteDao.getMutes(punishmentId)
        return mutesToMap(mutes, context)
    }

    private suspend fun mutesToMap(mutes: List<Mute>, context: ICommandContext): Map<Long, String> {
        val map = hashMapOf<Long, String>()
        if (mutes.isEmpty()) return emptyMap()
        mutes.forEach { mute ->
            val message = convertMuteInfoToMessage(context, mute)
            map[mute.startTime] = message
        }
        return map
    }

    private suspend fun convertMuteInfoToMessage(context: ICommandContext, mute: Mute): String {
        val muteAuthorId = mute.muteAuthorId ?: return continueConvertingInfoToMessage(context, null, mute)

        val muteAuthor = context.shardManager.retrieveUserById(muteAuthorId).awaitOrNull()
        return continueConvertingInfoToMessage(context, muteAuthor, mute)
    }

    private suspend fun continueConvertingInfoToMessage(
        context: ICommandContext,
        muteAuthor: User?,
        mute: Mute
    ): String {
        val unbanAuthorId = mute.unmuteAuthorId ?: return getMuteMessage(context, muteAuthor, null, mute)

        val unmuteAuthor = context.shardManager.retrieveUserById(unbanAuthorId).awaitOrNull()
        return getMuteMessage(context, muteAuthor, unmuteAuthor, mute)
    }

    private suspend fun getMuteMessage(
        context: ICommandContext,
        muteAuthor: User?,
        unmuteAuthor: User?,
        mute: Mute
    ): String {
        val deletedUser = context.getTranslation("message.deleted.user")
        val unmuteReason = mute.unmuteReason
        val zoneId = context.getTimeZoneId()
        val muteDuration = mute.endTime?.let { endTime ->
            getDurationString((endTime - mute.startTime))
        } ?: context.getTranslation("infinite")

        return context.getTranslation("message.punishmenthistory.mute")
            .withSafeVarInCodeblock("muteAuthor", muteAuthor?.asTag ?: deletedUser)
            .withVariable("muteAuthorId", "${mute.muteAuthorId}")
            .withSafeVarInCodeblock(
                "unmuteAuthor",
                if (mute.unmuteAuthorId == null) "/" else unmuteAuthor?.asTag ?: deletedUser
            )
            .withVariable("unmuteAuthorId", mute.unmuteAuthorId?.toString() ?: "/")
            .withSafeVarInCodeblock("muteReason", mute.reason.substring(0, min(mute.reason.length, 830)))
            .withSafeVarInCodeblock("unmuteReason", unmuteReason?.substring(0, min(unmuteReason.length, 830)) ?: "/")
            .withVariable("startTime", mute.startTime.asEpochMillisToDateTime(zoneId))
            .withVariable("endTime", mute.endTime?.asEpochMillisToDateTime(zoneId) ?: "/")
            .withSafeVarInCodeblock("duration", muteDuration)
            .withVariable("muteId", mute.muteId)
            .withVariable("active", "${mute.active}")
    }

    fun clear(guildId: Long, mutedId: Long, clearActive: Boolean) {
        if (clearActive) {
            muteDao.clearHistory(guildId, mutedId)
        } else {
            muteDao.clear(guildId, mutedId)
        }
    }

    override suspend fun getPunishments(guildId: Long, targetId: Long): List<Mute> {
        return muteDao.getMutes(guildId, targetId)
    }

    fun remove(mute: Mute) {
        muteDao.remove(mute)
    }
}