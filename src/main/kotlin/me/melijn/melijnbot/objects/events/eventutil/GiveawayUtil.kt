package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.jagtag.parsers.WelcomeJagTagParser
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.objects.utils.sendAttachmentsAwaitN
import me.melijn.melijnbot.objects.utils.sendMsgAwaitN
import me.melijn.melijnbot.objects.utils.sendMsgWithAttachmentsAwaitN
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl

object GiveawayUtil {
    suspend fun postGiveawayMessage(daoManager: DaoManager, member: Member, channelType: ChannelType, messageType: MessageType) {
        postGiveawayMessage(daoManager, member.guild, member.user, channelType, messageType)
    }

    suspend fun postGiveawayMessage(daoManager: DaoManager, guild: Guild, user: User, channelType: ChannelType, messageType: MessageType) {
        val guildId = guild.idLong

        val channel = guild.getAndVerifyChannelByType(daoManager, channelType, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
            ?: return

        val messageWrapper = daoManager.messageWrapper
        var modularMessage = messageWrapper.messageCache.get(Pair(guildId, messageType)).await() ?: return
        if (MessageCommandUtil.removeMessageIfEmpty(guildId, messageType, modularMessage, messageWrapper)) return

        modularMessage = replaceVariablesInGiveawayMessage(guild, user, modularMessage)

        val message: Message? = modularMessage.toMessage()
        when {
            message == null -> sendAttachmentsAwaitN(channel, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachmentsAwaitN(channel, message, modularMessage.attachments)
            else -> sendMsgAwaitN(channel, message)
        }
    }

    private suspend fun replaceVariablesInGiveawayMessage(guild: Guild, user: User, modularMessage: ModularMessage): ModularMessage {
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
        return newMessage
    }
}