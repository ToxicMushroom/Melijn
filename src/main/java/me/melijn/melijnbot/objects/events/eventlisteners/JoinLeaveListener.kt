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
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelById
import me.melijn.melijnbot.objects.utils.sendAttachments
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgWithAttachments
import net.dv8tion.jda.api.entities.*
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
        postWelcomeMessage(event.member, container, ChannelType.JOIN, MessageType.JOIN)
    }

    private fun onGuildMemberLeave(event: GuildMemberLeaveEvent) = CoroutineScope(Dispatchers.Default).launch {
        postWelcomeMessage(event.member, container, ChannelType.LEAVE, MessageType.LEAVE)
    }

    private suspend fun postWelcomeMessage(member: Member, container: Container, channelType: ChannelType, messageType: MessageType) {
        val guild = member.guild
        val guildId = guild.idLong
        val user = member.user
        val channelWrapper = container.daoManager.channelWrapper
        val channelId = channelWrapper.channelCache.get(Pair(guildId, channelType)).await()
        val channel = guild.getAndVerifyChannelById(channelType, channelId, channelWrapper) ?: return

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

    private fun replaceVariablesInWelcomeMessage(member: Member, modularMessage: ModularMessage): ModularMessage {
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

    private fun replaceWelcomeVariables(s: String, guild: Guild, user: User): String =
            s.replace("%userMention%".toRegex(RegexOption.IGNORE_CASE), user.asMention)
                    .replace("%guildName%".toRegex(RegexOption.IGNORE_CASE), guild.name)
                    .replace("%memberCount%".toRegex(RegexOption.IGNORE_CASE), guild.members.size.toString())
                    .replace("%userName%".toRegex(RegexOption.IGNORE_CASE), user.name)
                    .replace("%userDiscrim%".toRegex(RegexOption.IGNORE_CASE), user.discriminator)
                    .replace("%timeStamp%".toRegex(RegexOption.IGNORE_CASE), System.currentTimeMillis().asEpochMillisToDateTime())
                    .replace("%timeMillis%".toRegex(RegexOption.IGNORE_CASE), System.currentTimeMillis().toString())
                    .replace("%userId%".toRegex(RegexOption.IGNORE_CASE), user.id)
                    .replace("%guildId%".toRegex(RegexOption.IGNORE_CASE), guild.id)
                    .replace("%user%".toRegex(RegexOption.IGNORE_CASE), user.asTag)
                    .replace("%userAvatar%".toRegex(RegexOption.IGNORE_CASE), user.effectiveAvatarUrl)
                    .replace("%guildIcon%".toRegex(RegexOption.IGNORE_CASE), guild.iconUrl ?: MISSING_IMAGE_URL)
                    .replace("%bannerUrl%".toRegex(RegexOption.IGNORE_CASE), guild.bannerUrl ?: MISSING_IMAGE_URL)
                    .replace("%splashUrl%".toRegex(RegexOption.IGNORE_CASE), guild.splashUrl ?: MISSING_IMAGE_URL)
                    .replace("%vanityUrl%".toRegex(RegexOption.IGNORE_CASE), guild.vanityUrl ?: MISSING_IMAGE_URL)
}