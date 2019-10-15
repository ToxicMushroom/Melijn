package me.melijn.melijnbot.objects.utils

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User

object VerificationUtils {

    //guildId, userId, time
    private val memberJoinTimes = HashMap<Long, HashMap<Long, Long>>()

    suspend fun getUnverifiedRoleNMessage(user: User?, textChannel: TextChannel, daoManager: DaoManager): Role? {
        val role = getUnverifiedRoleN(textChannel, daoManager)

        if (role == null) {
            sendNoUnverifiedRoleIsSetMessage(daoManager, user, textChannel)
        }

        return role
    }

    suspend fun getUnverifiedRoleN(textChannel: TextChannel, daoManager: DaoManager): Role? {
        val guild = textChannel.guild
        val unverifiedRoleId = daoManager.roleWrapper.roleCache[Pair(guild.idLong, RoleType.UNVERIFIED)].await()

        return if (unverifiedRoleId == -1L) {
            null
        } else {
            guild.getRoleById(unverifiedRoleId)
        }
    }

    private suspend fun sendNoUnverifiedRoleIsSetMessage(daoManager: DaoManager, user: User?, textChannel: TextChannel) {
        val language = getLanguage(daoManager, user?.idLong ?: -1L, textChannel.guild.idLong)
        val msg = i18n.getTranslation(language, "message.notset.role.unverified")
        sendMsg(textChannel, msg)
    }

    suspend fun verify(daoManager: DaoManager, unverifiedRole: Role, member: Member) {
        if (hasHitThroughputLimit(daoManager, member)) {
            LogUtils.sendHitVerificationTroughputLimitLog(daoManager, member)
            return
        }
        val guild = unverifiedRole.guild
        guild.removeRoleFromMember(member, unverifiedRole)
            .reason("verified")
            .queue()

        LogUtils.sendVerifiedUserLog(daoManager, member)
    }

    suspend fun failedVerification(dao: DaoManager, member: Member) {
        LogUtils.sendFailedVerificationLog(dao, member)
    }


    private suspend fun hasHitThroughputLimit(daoManager: DaoManager, member: Member): Boolean {
        val guild = member.guild
        val max = daoManager.verificationUserFlowRateWrapper.verificationUserFlowRateCache[guild.idLong].await()
        val lastMembers = memberJoinTimes
            .getOrDefault(guild.idLong, emptyMap<Long, Long>())

        val lastHourJoinedMembersAmount = lastMembers.filter { (System.currentTimeMillis() - it.value) < 3_600_000 }.count()
        return lastHourJoinedMembersAmount >= max
    }


    suspend fun addUnverified(member: Member, daoManager: DaoManager) {
        val guild = member.guild
        val role = guild.getAndVerifyRoleByType(RoleType.UNVERIFIED, daoManager.roleWrapper, true) ?: return
        val result = guild.addRoleToMember(member, role).awaitNE()
        if (result == null) {
            val logChannel = guild.getAndVerifyLogChannelByType(LogChannelType.VERIFICATION, daoManager.logChannelWrapper)
            if (logChannel != null) {
                sendMessageFailedToAddRoleToMember(member, role, daoManager)
            }
        } else {
            daoManager.unverifiedUsersWrapper.add(member.guild.idLong, member.idLong)
        }
    }

    private suspend fun sendMessageFailedToAddRoleToMember(member: Member, role: Role, daoManager: DaoManager) {
        val channel = member.guild.getAndVerifyLogChannelByType(LogChannelType.VERIFICATION, daoManager.logChannelWrapper) ?: return

        val language = getLanguage(daoManager, -1, member.guild.idLong)
        val msg = i18n.getTranslation(language, "message.logging.verification.failedaddingrole")
            .replace("%userId%", member.id)
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace(PLACEHOLDER_ROLE, role.name)
            .replace("%roleId%", role.id)

        sendMsg(channel, msg)
    }


}