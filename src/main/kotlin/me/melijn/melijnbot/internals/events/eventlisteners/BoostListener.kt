package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.jagtag.WelcomeJagTagParser
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.message.sendAttachments
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgWithAttachments
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl

class BoostListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildUpdateBoostCountEvent) {
            TaskManager.async(event.guild) { onBoost(event) }
        }
    }

    private suspend fun onBoost(event: GuildUpdateBoostCountEvent) {
        val guild = event.guild
        val daoManager = container.daoManager
        val channelType = ChannelType.BOOST
        val messageType = MessageType.BOOST
        val guildId = guild.idLong

        val channel = guild.getAndVerifyChannelByType(daoManager, channelType, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                ?: return

        val messageWrapper = daoManager.messageWrapper
        var modularMessage = messageWrapper.getMessage(guildId, messageType) ?: return
        if (MessageCommandUtil.removeMessageIfEmpty(guildId, messageType, modularMessage, messageWrapper)) return

        val boosted = event.guild
            .findMembers { it.timeBoosted != null }
            .await()
            .maxByOrNull {
                it.timeBoosted?.toInstant()?.toEpochMilli() ?: 0
            } ?: return





        modularMessage = replaceVariablesInBoostMessage(guild, boosted, modularMessage)

        val message: Message? = modularMessage.toMessage()
        when {
            message == null -> sendAttachments(channel, container.webManager.proxiedHttpClient, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachments(channel, container.webManager.proxiedHttpClient, message, modularMessage.attachments)
            else -> sendMsg(channel, message)
        }
    }

    private suspend fun replaceVariablesInBoostMessage(guild: Guild, booster: Member, modularMessage: ModularMessage): ModularMessage {
        val newMessage = ModularMessage()
        val user = booster.user

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