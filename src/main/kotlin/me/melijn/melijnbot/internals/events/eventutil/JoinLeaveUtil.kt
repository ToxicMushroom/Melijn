package me.melijn.melijnbot.internals.events.eventutil

import io.ktor.client.*
import me.melijn.melijnbot.commandutil.administration.MessageUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.role.JoinRoleInfo
import me.melijn.melijnbot.database.role.UserType
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.jagtag.WelcomeJagTagParser
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.services.messagedeletion.MessageDeletionService
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.awaitBool
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.isPremiumGuild
import me.melijn.melijnbot.internals.utils.message.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import kotlin.random.Random

object JoinLeaveUtil {

    suspend fun postWelcomeMessage(
        daoManager: DaoManager,
        httpClient: HttpClient,
        member: Member,
        channelType: ChannelType,
        messageType: MessageType
    ) {
        postWelcomeMessage(daoManager, httpClient, member.guild, member.user, channelType, messageType)
    }

    suspend fun postWelcomeMessage(
        daoManager: DaoManager,
        httpClient: HttpClient,
        guild: Guild,
        user: User,
        channelType: ChannelType,
        messageType: MessageType
    ) {
        val guildId = guild.idLong

        var channel = guild.getAndVerifyChannelByType(
            daoManager, channelType, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ
        ) ?: return

        val messageWrapper = daoManager.messageWrapper
        val linkedMessageWrapper = daoManager.linkedMessageWrapper
        val msgName = linkedMessageWrapper.getMessage(guildId, messageType) ?: return
        var modularMessage = messageWrapper.getMessage(guildId, msgName) ?: return
        if (MessageUtil.removeMessageIfEmpty(guildId, messageType, modularMessage, linkedMessageWrapper)) return

        modularMessage = replaceVariablesInWelcomeMessage(guild, user, modularMessage)

        val message: Message? = modularMessage.toMessage()
        if (message?.embeds?.isNotEmpty() == true) {
            channel = guild.getAndVerifyChannelByType(daoManager, channelType, Permission.MESSAGE_EMBED_LINKS) ?: return
        }
        val wrapper = daoManager.autoRemoveInactiveJoinMessageWrapper
        val premiumGuild = isPremiumGuild(daoManager, guildId)
        if (premiumGuild &&
            listOf(MessageType.JOIN, MessageType.PRE_VERIFICATION_JOIN).contains(messageType)
        ) {
            val duration = wrapper.get(guildId)
            if (duration > 0) {
                val msg = when {
                    message == null -> sendAttachmentsAwaitN(channel, httpClient, modularMessage.attachments)
                    modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachmentsAwaitN(
                        channel,
                        httpClient,
                        message,
                        modularMessage.attachments
                    )
                    else -> sendMsgAwaitN(channel, message)
                }
                msg?.let {
                    val channelMsgPair = it.channel.idLong to it.idLong
                    daoManager.inactiveJMWrapper.new(
                        guildId, user.idLong, channelMsgPair, duration
                    )
                }
                return
            }

        } else if (premiumGuild) {
            val channelMsgPair = daoManager.inactiveJMWrapper.getMsg(guildId, user.idLong)
            if (channelMsgPair != null) {
                val text = guild.getTextChannelById(channelMsgPair.first)
                if (text != null) {
                    MessageDeletionService.queueMessageDeletes(text, channelMsgPair.second)
                    return
                }
            }
        }

        when {
            message == null -> sendAttachments(channel, httpClient, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachments(
                channel,
                httpClient,
                message,
                modularMessage.attachments
            )
            else -> sendMsg(channel, message)
        }
    }

    private suspend fun replaceVariablesInWelcomeMessage(
        guild: Guild,
        user: User,
        modularMessage: ModularMessage
    ): ModularMessage {
        return modularMessage.mapAllStringFields {
            if (it != null) {
                WelcomeJagTagParser.parseJagTag(guild, user, it)
            } else {
                null
            }
        }
    }

    suspend fun joinRole(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES)) return

        val groups = daoManager.joinRoleGroupWrapper.getList(guild.idLong)
        val joinRoleInfo = daoManager.joinRoleWrapper.getJRI(guild.idLong)
        val map = joinRoleInfo.dataMap
        for ((groupName, list) in map) {
            val group = groups.firstOrNull { it.groupName == groupName } ?: continue
            if (!group.isEnabled) continue
            val acceptedTypes = UserType.setFromInt(group.forUserTypes)
            val isBot = member.user.isBot
            if (isBot && !acceptedTypes.contains(UserType.BOT)) continue
            else if (!isBot && !acceptedTypes.contains(UserType.USER)) continue
            if (group.getAllRoles) {


                for ((roleId) in list) {
                    val role = roleId?.let { guild.getRoleById(it) } ?: continue
                    if (guild.selfMember.canInteract(role)) {
                        guild.addRoleToMember(member, role).reason("joinrole ${group.groupName}").queue()
                    } else {
                        LogUtils.sendMessageFailedToAddRoleToMember(daoManager, member, role)
                    }
                }
            } else {
                var totalChance = 0
                for (entry in list) {
                    totalChance += entry.chance
                }

                val winner = Random.nextInt(0, totalChance)
                var counter = 0
                var entryWon: JoinRoleInfo.JoinRoleEntry? = null
                for (entry in list) {
                    if (counter <= winner && (counter + entry.chance) > winner) {
                        entryWon = entry
                    }

                    counter += entry.chance
                }

                val immutableEntry = entryWon ?: continue

                val role = immutableEntry.roleId?.let { guild.getRoleById(it) } ?: continue
                if (guild.selfMember.canInteract(role)) {
                    guild.addRoleToMember(member, role).reason("joinrole $group").queue()
                } else {
                    LogUtils.sendMessageFailedToAddRoleToMember(daoManager, member, role)
                }
            }
        }
    }

    suspend fun forceRole(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        if (!guild.selfMember.canInteract(member)) return
        val wrapper = daoManager.forceRoleWrapper

        val map = wrapper.getForceRoles(guild.idLong)
        val roleIds = map.getOrDefault(member.idLong, emptyList())
        for (roleId in roleIds) {
            val role = guild.getRoleById(roleId)
            if (role == null) {
                wrapper.remove(guild.idLong, member.idLong, roleId)
                continue
            }
            if (!guild.selfMember.canInteract(role)) {
                continue
            }
            guild.addRoleToMember(member, role).reason("forcerole").queue()
        }
    }

    suspend fun reAddMute(daoManager: DaoManager, event: GuildMemberJoinEvent) {
        val mute = daoManager.muteWrapper.getActiveMute(event.guild.idLong, event.user.idLong) ?: return
        if (mute.endTime ?: 0 > System.currentTimeMillis()) {
            val muteRole = event.guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE, true) ?: return
            event.guild
                .addRoleToMember(event.member, muteRole)
                .reason("muted but rejoined")
                .awaitBool()
        }
    }
}