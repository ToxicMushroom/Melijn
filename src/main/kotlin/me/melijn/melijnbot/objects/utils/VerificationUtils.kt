package me.melijn.melijnbot.objects.utils

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.events.eventutil.JoinLeaveUtil
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
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

    suspend fun verify(daoManager: DaoManager, unverifiedRole: Role, author: User, member: Member) {
        if (hasHitThroughputLimit(daoManager, member)) {
            LogUtils.sendHitVerificationThroughputLimitLog(daoManager, member)
            return
        }
        val guild = unverifiedRole.guild
        guild.removeRoleFromMember(member, unverifiedRole)
            .reason("verified")
            .queue()

        LogUtils.sendVerifiedUserLog(daoManager, author, member)
        JoinLeaveUtil.postWelcomeMessage(daoManager, member, ChannelType.JOIN, MessageType.JOIN)
        JoinLeaveUtil.forceRole(daoManager, member)
    }

    suspend fun failedVerification(dao: DaoManager, member: Member) {
        val guild = member.guild
        val tries = dao.unverifiedUsersWrapper.getTries(guild.idLong, member.idLong)
        dao.unverifiedUsersWrapper.update(guild.idLong, member.idLong, tries + 1)
        LogUtils.sendFailedVerificationLog(dao, member)
    }


    private suspend fun hasHitThroughputLimit(daoManager: DaoManager, member: Member): Boolean {
        val guild = member.guild
        val max = daoManager.verificationUserFlowRateWrapper.verificationUserFlowRateCache[guild.idLong].await()
        if (max == -1L) return false
        val lastMembers = memberJoinTimes
            .getOrDefault(guild.idLong, emptyMap<Long, Long>())

        val lastHourJoinedMembersAmount = lastMembers.filter {
            (System.currentTimeMillis() - it.value) < 60_000
        }.size
        return lastHourJoinedMembersAmount >= max
    }


    suspend fun addUnverified(member: Member, daoManager: DaoManager) {
        val guild = member.guild
        val role = guild.getAndVerifyRoleByType(RoleType.UNVERIFIED, daoManager.roleWrapper, true) ?: return
        val result = guild.addRoleToMember(member, role).awaitBool()
        if (result) {
            daoManager.unverifiedUsersWrapper.add(member.guild.idLong, member.idLong)

        } else {
            LogUtils.sendMessageFailedToAddRoleToMember(daoManager, member, role)
        }
    }

    suspend fun isVerified(daoManager: DaoManager, member: Member): Boolean {
        val guild = member.guild
        val role = guild.getAndVerifyRoleByType(RoleType.UNVERIFIED, daoManager.roleWrapper, true) ?: return true
        return !(member.roles.contains(role))
    }


}