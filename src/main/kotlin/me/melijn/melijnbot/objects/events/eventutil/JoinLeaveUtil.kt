package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.jagtag.WelcomeJagTagParser
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.objects.utils.sendAttachments
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgWithAttachments
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl

object JoinLeaveUtil {
    suspend fun postWelcomeMessage(daoManager: DaoManager, member: Member, channelType: ChannelType, messageType: MessageType) {
        val guild = member.guild
        val guildId = guild.idLong

        val channel = guild.getAndVerifyChannelByType(channelType, daoManager, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
            ?: return

        val messageWrapper = daoManager.messageWrapper
        var modularMessage = messageWrapper.messageCache.get(Pair(guildId, messageType)).await() ?: return
        if (MessageCommandUtil.removeMessageIfEmpty(guildId, messageType, modularMessage, messageWrapper)) return

        modularMessage = replaceVariablesInWelcomeMessage(member, modularMessage)

        val message: Message? = modularMessage.toMessage()
        when {
            message == null -> sendAttachments(channel, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachments(channel, message, modularMessage.attachments)
            else -> sendMsg(channel, message)
        }
    }

    private suspend fun replaceVariablesInWelcomeMessage(member: Member, modularMessage: ModularMessage): ModularMessage {
        val newMessage = ModularMessage()

        newMessage.messageContent = modularMessage.messageContent?.let {
            WelcomeJagTagParser.parseJagTag(member, it)
        }

        val oldEmbedData = modularMessage.embed?.toData()
            ?.put("type", EmbedType.RICH)
        if (oldEmbedData != null) {
            val newEmbedJSON = WelcomeJagTagParser.parseJagTag(member, oldEmbedData.toString())
            val newEmbedData = DataObject.fromJson(newEmbedJSON)
            val newEmbed = (member.jda as JDAImpl).entityBuilder.createMessageEmbed(newEmbedData)
            newMessage.embed = newEmbed
        }


        val newAttachments = mutableMapOf<String, String>()
        modularMessage.attachments.forEach { (t, u) ->
            val url = WelcomeJagTagParser.parseJagTag(member, t)
            val file = WelcomeJagTagParser.parseJagTag(member, u)
            newAttachments[url] = file
        }
        newMessage.attachments = newAttachments
        return newMessage

    }

    suspend fun forceRole(daoManager: DaoManager, event: GuildMemberJoinEvent) {
        val member = event.member
        val guild = event.guild
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
            guild.addRoleToMember(member, role).queue()
        }
    }
}