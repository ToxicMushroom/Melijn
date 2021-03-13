package me.melijn.melijnbot.internals.utils

import io.ktor.client.*
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.events.eventutil.JoinLeaveUtil
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.message.sendRspOrMsg
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.HierarchyException

object VerificationUtils {

    //guildId, userId, time
    private val memberJoinTimes = HashMap<Long, HashMap<Long, Long>>()

    suspend fun getUnverifiedRoleNMessage(
        user: User?,
        textChannel: TextChannel,
        daoManager: DaoManager,
        prefix: String
    ): Role? {
        val role = getUnverifiedRoleN(textChannel, daoManager)

        if (role == null) {
            sendNoUnverifiedRoleIsSetMessage(daoManager, user, textChannel, prefix)
        }

        return role
    }

    suspend fun getUnverifiedRoleN(textChannel: TextChannel, daoManager: DaoManager): Role? {
        val guild = textChannel.guild
        val unverifiedRoleId = daoManager.roleWrapper.getRoleId(guild.idLong, RoleType.UNVERIFIED)

        return if (unverifiedRoleId == -1L) {
            null
        } else {
            guild.getRoleById(unverifiedRoleId)
        }
    }

    private suspend fun sendNoUnverifiedRoleIsSetMessage(
        daoManager: DaoManager,
        user: User?,
        textChannel: TextChannel,
        prefix: String
    ) {
        val language = getLanguage(daoManager, user?.idLong ?: -1L, textChannel.guild.idLong)
        val msg = i18n.getTranslation(language, "message.notset.role.unverified")
            .withVariable("prefix", prefix)

        sendRspOrMsg(textChannel, daoManager, msg)
    }

    suspend fun verify(
        daoManager: DaoManager,
        httpClient: HttpClient,
        unverifiedRole: Role,
        author: User,
        member: Member
    ): Boolean {
        if (hasHitThroughputLimit(daoManager, member)) {
            LogUtils.sendHitVerificationThroughputLimitLog(daoManager, member)
            return false
        }
        val guild = unverifiedRole.guild
        if (unverifiedRole.idLong != guild.idLong) {
            val result = try {
                guild.removeRoleFromMember(member, unverifiedRole)
                    .reason("verified")
                    .awaitBool()
            } catch (t: HierarchyException) {
                false
            }
            if (!result) {
                LogUtils.sendMessageFailedToRemoveRoleFromMember(daoManager, member, unverifiedRole)
                return false
            }
        }

        daoManager.unverifiedUsersWrapper.remove(member.guild.idLong, member.idLong)
        LogUtils.sendVerifiedUserLog(daoManager, author, member)
        JoinLeaveUtil.postWelcomeMessage(daoManager, httpClient, member, ChannelType.JOIN, MessageType.JOIN)
        JoinLeaveUtil.forceRole(daoManager, member)
        JoinLeaveUtil.joinRole(daoManager, member)
        return true
    }

    suspend fun failedVerification(dao: DaoManager, member: Member) {
        val guild = member.guild
        val tries = dao.unverifiedUsersWrapper.getTries(guild.idLong, member.idLong)
        dao.unverifiedUsersWrapper.update(guild.idLong, member.idLong, tries + 1)
        LogUtils.sendFailedVerificationLog(dao, member)
    }


    private suspend fun hasHitThroughputLimit(daoManager: DaoManager, member: Member): Boolean {
        val guild = member.guild
        val max = daoManager.verificationUserFlowRateWrapper.getFlowRate(guild.idLong)
        if (max == -1L) return false
        val lastMembers = memberJoinTimes
            .getOrDefault(guild.idLong, emptyMap())

        val lastHourJoinedMembersAmount = lastMembers.filter {
            (System.currentTimeMillis() - it.value) < 60_000
        }.size
        return lastHourJoinedMembersAmount >= max
    }


    suspend fun addUnverified(member: Member, httpClient: HttpClient, daoManager: DaoManager) {
        val guild = member.guild
        JoinLeaveUtil.postWelcomeMessage(
            daoManager,
            httpClient,
            member,
            ChannelType.PRE_VERIFICATION_JOIN,
            MessageType.PRE_VERIFICATION_JOIN
        )

        val role = guild.getAndVerifyRoleByType(daoManager, RoleType.UNVERIFIED, true) ?: return

        val result = if (role.idLong != guild.idLong) {
            guild.addRoleToMember(member, role)
                .reason("unverified")
                .awaitBool()
        } else {
            true
        }
        if (result) {
            daoManager.unverifiedUsersWrapper.add(member.guild.idLong, member.idLong)
        } else {
            LogUtils.sendMessageFailedToAddRoleToMember(daoManager, member, role)
        }
    }

    suspend fun isVerified(daoManager: DaoManager, member: Member): Boolean {

        val guild = member.guild
        val role = guild.getAndVerifyRoleByType(daoManager, RoleType.UNVERIFIED, true) ?: return true
        return !(member.roles.contains(role))
    }
}