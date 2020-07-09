package me.melijn.melijnbot.internals.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.FilterUtil
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.escapeForLog
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import java.awt.Color

class MessageUpdateListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) = runBlocking {
        if (event is GuildMessageUpdateEvent) {
            onMessageUpdate(event)
            FilterUtil.handleFilter(container, event.message)
        }
    }

    private suspend fun onMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.author.isBot) return
        val message = event.message
        val newContent = event.message.contentRaw
        val guild = event.guild
        val daoManager = container.daoManager
        val messageWrapper = daoManager.messageHistoryWrapper
        val deferredMessage = messageWrapper.getMessageById(event.messageIdLong)

        val daoMessage = deferredMessage
            ?: DaoMessage(
                guild.idLong,
                event.channel.idLong,
                event.author.idLong,
                message.idLong,
                newContent,
                message.timeCreated.toInstant().toEpochMilli()
            )

        val oldContent = daoMessage.content
        daoMessage.content = newContent

        container.taskManager.async {
            messageWrapper.setMessage(daoMessage)

            val channel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.EDITED_MESSAGE, true)
                ?: return@async
            postMessageUpdateLog(event, channel, daoMessage, oldContent)
        }
    }

    private suspend fun postMessageUpdateLog(event: GuildMessageUpdateEvent, logChannel: TextChannel, daoMessage: DaoMessage, oldContent: String) {
        val embedBuilder = EmbedBuilder()
        val daoManager = container.daoManager
        val zoneId = getZoneId(daoManager, event.guild.idLong)
        val language = getLanguage(daoManager, -1, event.guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.update.log.title")
            .withVariable(PLACEHOLDER_CHANNEL, event.channel.asTag)

        val description = i18n.getTranslation(language, "listener.message.update.log.description")
            .withVariable("oldContent", escapeForLog(oldContent))
            .withVariable("newContent", escapeForLog(daoMessage.content))
            .withVariable(PLACEHOLDER_USER, event.author.asTag)
            .withVariable(PLACEHOLDER_USER_ID, event.author.id)
            .withVariable("sentTime", event.message.timeCreated.asLongLongGMTString())
            .withVariable("editedTime", System.currentTimeMillis().asEpochMillisToDateTime(zoneId))
            .withVariable("link", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.message.id}")

        embedBuilder.setColor(Color.decode("#A1DAC3"))
        embedBuilder.setThumbnail(event.author.effectiveAvatarUrl)

        embedBuilder.setTitle(title)

        if (description.length > 2048) {
            val parts = StringUtils.splitMessageWithCodeBlocks(description, lang = "LDIF").toMutableList()
            embedBuilder.setDescription(parts[0])
            sendEmbed(daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
            embedBuilder.setTitle(null)
            embedBuilder.setThumbnail(null)
            parts.removeAt(0)

            for (part in parts) {
                embedBuilder.setDescription(part)
                sendEmbed(daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
            }
        } else {
            embedBuilder.setDescription(description)

            sendEmbed(daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
        }
    }
}