package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.jagtag.WelcomeJagTagParser
import me.melijn.melijnbot.objects.utils.VerificationUtils
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.objects.utils.sendAttachments
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgWithAttachments
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl

class JoinLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMemberJoinEvent) onGuildMemberJoin(event)
        else if (event is GuildMemberLeaveEvent) onGuildMemberLeave(event)
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) = CoroutineScope(Dispatchers.Default).launch {
        if (noVerification(event)) {
            postWelcomeMessage(event.member, ChannelType.JOIN, MessageType.JOIN)
            forceRole(event)
        } else {
            VerificationUtils.addUnverifiedRole(event.member, container.daoManager.roleWrapper)
        }
    }

    private suspend fun noVerification(event: GuildMemberJoinEvent): Boolean {
        val channel = event.guild.getAndVerifyChannelByType(ChannelType.VERIFICATION, container.daoManager.channelWrapper)
        return channel == null
    }

    private fun onGuildMemberLeave(event: GuildMemberLeaveEvent) = CoroutineScope(Dispatchers.Default).launch {
        postWelcomeMessage(event.member, ChannelType.LEAVE, MessageType.LEAVE)
    }


    private suspend fun forceRole(event: GuildMemberJoinEvent) {
        val member = event.member
        val guild = event.guild
        if (!guild.selfMember.canInteract(member)) return
        val wrapper = container.daoManager.forceRoleWrapper

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


    private suspend fun postWelcomeMessage(member: Member, channelType: ChannelType, messageType: MessageType) {
        val guild = member.guild
        val guildId = guild.idLong
        val channelWrapper = container.daoManager.channelWrapper

        val channel = guild.getAndVerifyChannelByType(channelType, channelWrapper) ?: return

        val messageWrapper = container.daoManager.messageWrapper
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
}