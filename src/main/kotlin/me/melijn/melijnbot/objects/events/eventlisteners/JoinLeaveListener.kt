package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.JoinLeaveUtil
import me.melijn.melijnbot.objects.events.eventutil.JoinLeaveUtil.joinRole
import me.melijn.melijnbot.objects.utils.VerificationUtils
import me.melijn.melijnbot.objects.utils.awaitOrNull
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class JoinLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMemberJoinEvent) onGuildMemberJoin(event)
        else if (event is GuildMemberRemoveEvent) onGuildMemberLeave(event)
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) = container.taskManager.async {
        val daoManager = container.daoManager
        val member = event.member
        JoinLeaveUtil.reAddMute(daoManager, event)

        if (guildHasNoVerification(event.guild)) {
            JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, member.user, ChannelType.JOIN, MessageType.JOIN)
            JoinLeaveUtil.forceRole(daoManager, member)
            joinRole(daoManager, member)

        } else {
            VerificationUtils.addUnverified(member, daoManager)

        }
    }

    private suspend fun guildHasNoVerification(guild: Guild): Boolean {
        val channel = guild.getAndVerifyChannelByType(container.daoManager, ChannelType.VERIFICATION)
        return channel == null
    }

    private fun onGuildMemberLeave(event: GuildMemberRemoveEvent) = container.taskManager.async {
        val daoManager = container.daoManager
        val user = event.user

        if (!daoManager.unverifiedUsersWrapper.contains(event.guild.idLong, user.idLong)) {
            if (event.guild.selfMember.hasPermission(Permission.BAN_MEMBERS, Permission.VIEW_AUDIT_LOGS)) {
                val stateAction = daoManager.bannedOrKickedTriggersLeaveWrapper.bannedOrKickedTriggersLeaveCache.get(event.guild.idLong)
                val ban = event.guild.retrieveBan(user).awaitOrNull()
                if (ban == null) {
                    val auditKick = event.guild.retrieveAuditLogs()
                        .type(ActionType.KICK)
                        .limit(5)
                        .awaitOrNull()



                    if (auditKick == null) {
                        JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.LEAVE, MessageType.LEAVE)
                    } else {
                        val now = OffsetDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.of("GMT"))
                        var kicked = false
                        for (entry in auditKick) {
                            if (entry.targetIdLong == user.idLong) {
                                if (OffsetDateTime.now().until(now, ChronoUnit.SECONDS) < 3) {
                                    kicked = true
                                    break
                                }
                            }
                        }

                        if (kicked) {
                            JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.KICKED, MessageType.KICKED)
                            if (stateAction.await()) {
                                JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.LEAVE, MessageType.LEAVE)
                            }
                        } else {
                            JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.LEAVE, MessageType.LEAVE)
                        }
                    }
                } else {
                    JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.BANNED, MessageType.BANNED)
                    if (stateAction.await()) {
                        JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.LEAVE, MessageType.LEAVE)
                    }
                }
            } else {
                JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.LEAVE, MessageType.LEAVE)

            }

        } else if (!guildHasNoVerification(event.guild)) {
            JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.PRE_VERIFICATION_LEAVE, MessageType.PRE_VERIFICATION_LEAVE_MESSAGE)

        }
    }
}