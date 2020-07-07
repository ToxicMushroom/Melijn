package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.database.role.JoinRoleInfo
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.jagtag.WelcomeJagTagParser
import me.melijn.melijnbot.objects.utils.LogUtils
import me.melijn.melijnbot.objects.utils.awaitBool
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.objects.utils.message.sendAttachments
import me.melijn.melijnbot.objects.utils.message.sendMsg
import me.melijn.melijnbot.objects.utils.message.sendMsgWithAttachments
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import kotlin.random.Random

object JoinLeaveUtil {

    suspend fun postWelcomeMessage(daoManager: DaoManager, member: Member, channelType: ChannelType, messageType: MessageType) {
        postWelcomeMessage(daoManager, member.guild, member.user, channelType, messageType)
    }

    suspend fun postWelcomeMessage(daoManager: DaoManager, guild: Guild, user: User, channelType: ChannelType, messageType: MessageType) {
        val guildId = guild.idLong

        val channel = guild.getAndVerifyChannelByType(daoManager, channelType, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
            ?: return

        val messageWrapper = daoManager.messageWrapper
        var modularMessage = messageWrapper.messageCache.get(Pair(guildId, messageType)).await() ?: return
        if (MessageCommandUtil.removeMessageIfEmpty(guildId, messageType, modularMessage, messageWrapper)) return

        modularMessage = replaceVariablesInWelcomeMessage(guild, user, modularMessage)

        val message: Message? = modularMessage.toMessage()
        when {
            message == null -> sendAttachments(channel, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachments(channel, message, modularMessage.attachments)
            else -> sendMsg(channel, message)
        }
    }

    private suspend fun replaceVariablesInWelcomeMessage(guild: Guild, user: User, modularMessage: ModularMessage): ModularMessage {
        val newMessage = ModularMessage()

        newMessage.messageContent = modularMessage.messageContent?.let {
            WelcomeJagTagParser.parseJagTag(guild, user, it)
        }

        val oldEmbedData = modularMessage.embed?.toData()
            ?.put("type", EmbedType.RICH)
        if (oldEmbedData != null) {
            val newEmbedJSON = WelcomeJagTagParser.parseJagTag(guild, user, oldEmbedData.toString())
            val newEmbedData = DataObject.fromJson(newEmbedJSON)
            val newEmbed = (user.jda as JDAImpl).entityBuilder.createMessageEmbed(newEmbedData)
            newMessage.embed = newEmbed
        }


        val newAttachments = mutableMapOf<String, String>()
        modularMessage.attachments.forEach { (t, u) ->
            val url = WelcomeJagTagParser.parseJagTag(guild, user, t)
            val file = WelcomeJagTagParser.parseJagTag(guild, user, u)
            newAttachments[url] = file
        }

        newMessage.attachments = newAttachments
        newMessage.extra = modularMessage.extra
        return newMessage
    }

    suspend fun joinRole(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        if (!guild.selfMember.canInteract(member)) return

        val groups = daoManager.joinRoleGroupWrapper.joinRoleGroupCache[guild.idLong].await()
        val joinRoleInfo = daoManager.joinRoleWrapper.joinRoleCache.get(guild.idLong).await()
        val map = joinRoleInfo.dataMap
        for ((groupName, list) in map) {
            val group = groups.firstOrNull { it.groupName == groupName } ?: continue
            if (!group.isEnabled) continue
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
                if (guild.selfMember.canInteract(member)) {
                    guild.addRoleToMember(member, role).reason("joinrole $group").queue()
                }
            }
        }
    }

    suspend fun forceRole(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        if (!guild.selfMember.canInteract(member)) return
        val wrapper = daoManager.forceRoleWrapper

        val map = wrapper.forceRoleCache.get(guild.idLong).await()
        val roleIds = map.getOrDefault(member.idLong, emptyList())
        for (roleId in roleIds) {
            val role = guild.getRoleById(roleId)
            if (role == null) {
                wrapper.remove(guild.idLong, member.idLong, roleId)
                continue
            }
            if (!member.canInteract(role)) {
                wrapper.remove(guild.idLong, member.idLong, roleId)
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