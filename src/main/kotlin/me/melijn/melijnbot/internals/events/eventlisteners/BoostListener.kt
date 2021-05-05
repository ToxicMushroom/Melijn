package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commandutil.administration.MessageUtil
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.jagtag.WelcomeJagTagParser
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.message.sendAttachments
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgWithAttachments
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateBoostCountEvent

class BoostListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
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

        val channel =
            guild.getAndVerifyChannelByType(daoManager, channelType, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)
                ?: return

        val messageWrapper = daoManager.messageWrapper
        val linkedMessageWrapper = daoManager.linkedMessageWrapper
        val msgName = linkedMessageWrapper.getMessage(guildId, messageType) ?: return
        var modularMessage = messageWrapper.getMessage(guildId, msgName) ?: return
        if (MessageUtil.removeMessageIfEmpty(guildId, messageType, modularMessage, linkedMessageWrapper)) return

        val boosted = event.guild
            .findMembers { it.timeBoosted != null }
            .await()
            .maxByOrNull {
                it.timeBoosted?.toInstant()?.toEpochMilli() ?: 0
            } ?: return

        // Workaround for people who boost twice
        if (System.currentTimeMillis() - (boosted.timeBoosted?.toInstant()?.toEpochMilli() ?: 0) > 600_000) {
            return
        }


        modularMessage = replaceVariablesInBoostMessage(guild, boosted, modularMessage, msgName)

        val message: Message? = modularMessage.toMessage()
        when {
            message == null -> sendAttachments(
                channel,
                container.webManager.proxiedHttpClient,
                modularMessage.attachments
            )
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachments(
                channel,
                container.webManager.proxiedHttpClient,
                message,
                modularMessage.attachments
            )
            else -> sendMsg(channel, message)
        }
    }

    private suspend fun replaceVariablesInBoostMessage(
        guild: Guild,
        booster: Member,
        modularMessage: ModularMessage,
        msgName: String
    ): ModularMessage {
        val user = booster.user

        return modularMessage.mapAllStringFieldsSafe("BoostMessage(msgName=$msgName)") {
            if (it != null) WelcomeJagTagParser.parseJagTag(guild, user, it)
            else null
        }
    }


}