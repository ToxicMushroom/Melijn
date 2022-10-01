package me.melijn.melijnbot.internals.events.eventlisteners

import io.ktor.client.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.JoinLeaveUtil
import me.melijn.melijnbot.internals.events.eventutil.JoinLeaveUtil.joinRole
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.VerificationUtils
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.isPremiumGuild
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class JoinLeaveListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildMemberJoinEvent) onGuildMemberJoin(event)
        else if (event is GuildMemberRemoveEvent) onGuildMemberLeave(event)
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) = TaskManager.async(event.member) {
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

    private fun onGuildMemberLeave(event: GuildMemberRemoveEvent): Job = TaskManager.async(event.user, event.guild) {
        val guild = event.guild
        val daoManager = container.daoManager
        val user = event.user

        val proxiedHttp = container.webManager.proxiedHttpClient
        val welcomeContext = WelcomeContext(daoManager, proxiedHttp, guild, user)

        val guildId = guild.idLong
        val userId = user.idLong

        if (!daoManager.unverifiedUsersWrapper.contains(guildId, userId)) {
            if (guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
                val stateAction: suspend () -> Boolean = {
                    daoManager.bannedOrKickedTriggersLeaveWrapper.shouldTrigger(guildId)
                }
                delay(3000) // wait for ban event to come through :)
                val isBanned = daoManager.bannedUsers.contains(userId, guildId) // ban event will inject this
                if (!isBanned) {
                    val auditKick = if (guild.selfMember.hasPermission(
                            Permission.VIEW_AUDIT_LOGS
                        ) && isPremiumGuild(daoManager, guildId)
                    ) {
                        guild.retrieveAuditLogs()
                            .type(ActionType.KICK)
                            .limit(5)
                            .awaitOrNull()
                    } else null

                    if (auditKick == null) {
                        postWelcome(welcomeContext, ChannelType.LEAVE)
                    } else {
                        val now = OffsetDateTime.now(ZoneOffset.UTC)
                        val kicked = auditKick.any { entry ->
                            if (entry.targetIdLong != userId) return@any false
                            return@any now.until(entry.timeCreated, ChronoUnit.SECONDS) < 3
                        }

                        if (kicked) {
                            postWelcome(welcomeContext, ChannelType.KICKED)
                            if (stateAction()) {
                                postWelcome(welcomeContext, ChannelType.LEAVE)
                            }
                        } else {
                            postWelcome(welcomeContext, ChannelType.LEAVE)
                        }
                    }
                } else {
                    postWelcome(welcomeContext, ChannelType.BANNED)
                    if (stateAction()) {
                        postWelcome(welcomeContext, ChannelType.LEAVE)
                    }
                }
            } else {
                postWelcome(welcomeContext, ChannelType.LEAVE)
            }

        } else if (!guildHasNoVerification(guild)) {
            postWelcome(welcomeContext, ChannelType.PRE_VERIFICATION_LEAVE)
        }
    }

    suspend fun postWelcome(welcomeContext: WelcomeContext, cType: ChannelType) {
        val messageType = when (cType) {
            ChannelType.PRE_VERIFICATION_LEAVE -> MessageType.PRE_VERIFICATION_LEAVE
            ChannelType.LEAVE -> MessageType.LEAVE
            ChannelType.BANNED -> MessageType.BANNED
            ChannelType.KICKED -> MessageType.KICKED
            else -> return
        }
        JoinLeaveUtil.postWelcomeMessage(
            welcomeContext.daoManager, welcomeContext.httpClient, welcomeContext.guild,
            welcomeContext.user, cType, messageType
        )
    }

    data class WelcomeContext(
        val daoManager: DaoManager,
        val httpClient: HttpClient,
        val guild: Guild,
        val user: User
    )
}