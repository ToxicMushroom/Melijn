package me.melijn.melijnbot.internals.events.eventlisteners

import lol.up.pylon.gateway.client.entity.Guild
import lol.up.pylon.gateway.client.entity.event.Event
import lol.up.pylon.gateway.client.entity.event.GuildMemberAddEvent
import lol.up.pylon.gateway.client.entity.event.GuildMemberRemoveEvent
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.events.SuspendListener
import me.melijn.melijnbot.internals.events.eventutil.JoinLeaveUtil
import me.melijn.melijnbot.internals.events.eventutil.JoinLeaveUtil.joinRole
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.VerificationUtils
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class JoinLeaveListener(val container: Container) : SuspendListener() {

    override suspend fun onEvent(event: Event<*>) {
        if (event is GuildMemberAddEvent) onGuildMemberJoin(event)
        else if (event is GuildMemberRemoveEvent) onGuildMemberLeave(event)
    }

    private fun onGuildMemberJoin(event: GuildMemberAddEvent) = TaskManager.async(event.member) {
        val daoManager = container.daoManager
        val member = event.member
        JoinLeaveUtil.reAddMute(daoManager, event)

        if (guildHasNoVerification(event.guild)) {
            JoinLeaveUtil.postWelcomeMessage(
                daoManager,
                container.webManager.proxiedHttpClient,
                event.guild,
                member.user,
                ChannelType.JOIN,
                MessageType.JOIN
            )
            JoinLeaveUtil.forceRole(daoManager, member)
            joinRole(daoManager, member)

        } else {
            VerificationUtils.addUnverified(member, container.webManager.proxiedHttpClient, daoManager)

        }
    }

    private suspend fun guildHasNoVerification(guild: Guild): Boolean {
        val channel = guild.getAndVerifyChannelByType(container.daoManager, ChannelType.VERIFICATION)
        return channel == null
    }

    private fun onGuildMemberLeave(event: GuildMemberRemoveEvent) = TaskManager.async(event.user, event.guild) {
        val daoManager = container.daoManager
        val user = event.user

        val proxiedHttp = container.webManager.proxiedHttpClient
        if (!daoManager.unverifiedUsersWrapper.contains(event.guild.idLong, user.idLong)) {
            if (event.guild.selfMember.hasPermission(Permission.BAN_MEMBERS, Permission.VIEW_AUDIT_LOGS)) {
                val stateAction: suspend () -> Boolean =
                    { daoManager.bannedOrKickedTriggersLeaveWrapper.shouldTrigger(event.guild.idLong) }
                val ban = event.guild.retrieveBan(user).awaitOrNull()
                if (ban == null) {
                    val auditKick = if (event.guild.selfMember.hasPermission(
                            Permission.BAN_MEMBERS,
                            Permission.VIEW_AUDIT_LOGS
                        )
                    ) {
                        event.guild.retrieveAuditLogs()
                            .type(ActionType.KICK)
                            .limit(5)
                            .awaitOrNull()
                    } else null


                    if (auditKick == null) {
                        JoinLeaveUtil.postWelcomeMessage(
                            daoManager,
                            proxiedHttp,
                            event.guild,
                            user,
                            ChannelType.LEAVE,
                            MessageType.LEAVE
                        )
                    } else {
                        var kicked = false
                        for (entry in auditKick) {
                            if (entry.targetIdLong == user.idLong) {
                                if (OffsetDateTime.now(ZoneOffset.UTC).until(entry.timeCreated, ChronoUnit.SECONDS) < 3) {
                                    kicked = true
                                    break
                                }
                            }
                        }

                        if (kicked) {
                            JoinLeaveUtil.postWelcomeMessage(
                                daoManager,
                                proxiedHttp,
                                event.guild,
                                user,
                                ChannelType.KICKED,
                                MessageType.KICKED
                            )
                            if (stateAction()) {
                                JoinLeaveUtil.postWelcomeMessage(
                                    daoManager,
                                    proxiedHttp,
                                    event.guild,
                                    user,
                                    ChannelType.LEAVE,
                                    MessageType.LEAVE
                                )
                            }
                        } else {
                            JoinLeaveUtil.postWelcomeMessage(
                                daoManager,
                                proxiedHttp,
                                event.guild,
                                user,
                                ChannelType.LEAVE,
                                MessageType.LEAVE
                            )
                        }
                    }
                } else {
                    JoinLeaveUtil.postWelcomeMessage(
                        daoManager,
                        proxiedHttp,
                        event.guild,
                        user,
                        ChannelType.BANNED,
                        MessageType.BANNED
                    )
                    if (stateAction()) {
                        JoinLeaveUtil.postWelcomeMessage(
                            daoManager,
                            proxiedHttp,
                            event.guild,
                            user,
                            ChannelType.LEAVE,
                            MessageType.LEAVE
                        )
                    }
                }
            } else {
                JoinLeaveUtil.postWelcomeMessage(
                    daoManager,
                    proxiedHttp,
                    event.guild,
                    user,
                    ChannelType.LEAVE,
                    MessageType.LEAVE
                )

            }

        } else if (!guildHasNoVerification(event.guild)) {
            JoinLeaveUtil.postWelcomeMessage(
                daoManager,
                proxiedHttp,
                event.guild,
                user,
                ChannelType.PRE_VERIFICATION_LEAVE,
                MessageType.PRE_VERIFICATION_LEAVE
            )
        }
    }
}