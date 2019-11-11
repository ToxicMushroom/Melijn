package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.asTag
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.audit.AuditLogOption
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import java.awt.Color
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors


class MessageDeletedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageDeleteEvent) {
            onGuildMessageDelete(event)
        }
    }

    private fun onGuildMessageDelete(event: GuildMessageDeleteEvent) = CoroutineScope(Dispatchers.Default).launch {
        val guild = event.guild
        val guildId = event.guild.idLong
        val logChannelWrapper = container.daoManager.logChannelWrapper
        val logChannelCache = logChannelWrapper.logChannelCache
        if (!guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) return@launch

        val odmId = logChannelCache.get(Pair(guildId, LogChannelType.OTHER_DELETED_MESSAGE)).await()
        val sdmId = logChannelCache.get(Pair(guildId, LogChannelType.SELF_DELETED_MESSAGE)).await()
        val pmId = logChannelCache.get(Pair(guildId, LogChannelType.PURGED_MESSAGE)).await()
        val fmId = logChannelCache.get(Pair(guildId, LogChannelType.FILTERED_MESSAGE)).await()
        if (odmId == -1L && sdmId == -1L && pmId == -1L && fmId == -1L) return@launch

        val odmLogChannel = getNLogChannel(event, odmId, LogChannelType.OTHER_DELETED_MESSAGE)
        val sdmLogChannel = getNLogChannel(event, sdmId, LogChannelType.SELF_DELETED_MESSAGE)
        val pmLogChannel = getNLogChannel(event, pmId, LogChannelType.PURGED_MESSAGE)
        val fmLogChannel = getNLogChannel(event, fmId, LogChannelType.FILTERED_MESSAGE)
        if (odmLogChannel == null && sdmLogChannel == null && pmLogChannel == null && fmLogChannel == null) return@launch

        selectCorrectLogType(event, odmLogChannel, sdmLogChannel, pmLogChannel, fmLogChannel)
    }

    private suspend fun getNLogChannel(
        event: GuildMessageDeleteEvent,
        logChannelId: Long?,
        logChannelType: LogChannelType,
        handleDeletedChannel: Boolean = true
    ): TextChannel? {
        val channel = if (logChannelId != null) {
            event.guild.getTextChannelById(logChannelId)
        } else {
            null
        }
        if (handleDeletedChannel &&
            ((channel == null && logChannelId != -1L) ||
                (channel != null && !event.guild.selfMember.hasPermission(channel, Permission.MESSAGE_WRITE)))
        ) {
            container.daoManager.logChannelWrapper.removeChannel(event.guild.idLong, logChannelType)
        }
        return channel
    }

    private suspend fun selectCorrectLogType(
        event: GuildMessageDeleteEvent,
        odmLogChannel: TextChannel?,
        sdmLogChannel: TextChannel?,
        pmLogChannel: TextChannel?,
        fmLogChannel: TextChannel?
    ) {
        val guild = event.guild
        val msg = container.daoManager.messageHistoryWrapper.getMessageById(event.messageIdLong) ?: return
        val msgDeleteTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(System.currentTimeMillis()), ZoneId.of("GMT"))

        when {
            container.purgedIds.keys.contains(msg.messageId) -> {
                val purgerId = container.purgedIds[msg.messageId] ?: return
                postDeletedByPurgeLog(pmLogChannel, msg, event, purgerId)
                container.purgedIds.remove(msg.messageId)
                return
            }
            container.filteredMap.keys.contains(msg.messageId) -> {
                postDeletedByFilterLog(fmLogChannel, msg, event, container.filteredMap[msg.messageId])
                container.filteredMap.remove(msg.messageId)
                return
            }
            container.botDeletedMessageIds.contains(msg.messageId) -> {
                postDeletedByOtherLog(odmLogChannel, msg, event, event.guild.selfMember)
                container.botDeletedMessageIds.remove(msg.messageId)
                println("Bot deleted this message")
                return
            }
            else -> {
                val list = guild.retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).limit(50).await()
                val filtered = list.stream()
                    .filter {
                        it.getOption<String>(AuditLogOption.CHANNEL)?.toLong() == msg.textChannelId &&
                            it.targetIdLong == msg.authorId &&
                            it.timeCreated.until(msgDeleteTime, ChronoUnit.MINUTES) <= 5
                    }
                    .collect(Collectors.toList())

                val entry = when {
                    filtered.size > 1 -> {
                        filtered.sortBy { logEntry ->
                            logEntry.timeCreated
                        }

                        filtered.asReversed()[0]
                    }
                    filtered.size == 1 -> filtered[0]
                    else -> null
                }

                if (entry != null) {
                    val user = entry.user ?: return
                    val member = event.guild.getMember(user) ?: return

                    postDeletedByOtherLog(odmLogChannel, msg, event, member)
                } else {
                    postDeletedBySelfLog(sdmLogChannel, msg, event)
                }
            }
        }
    }

    private suspend fun postDeletedByPurgeLog(pmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent, purgeRequesterId: Long) {
        if (pmLogChannel == null) return
        val messageAuthor = event.jda.shardManager?.retrieveUserById(msg.authorId)?.await() ?: return
        val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, event.jda.selfUser.idLong)
        eb.setColor(Color.decode("#551A8B"))

        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val footer = i18n.getTranslation(language, "listener.message.deletion.log.purged.footer")
            .replace(PLACEHOLDER_USER, messageAuthor.asTag)
            .replace(PLACEHOLDER_USER_ID, purgeRequesterId.toString())
        eb.setFooter(footer, messageAuthor.effectiveAvatarUrl)

        sendEmbed(container.daoManager.embedDisabledWrapper, pmLogChannel, eb.build())
    }

    private suspend fun postDeletedBySelfLog(sdmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent) {
        if (sdmLogChannel == null) return
        val messageAuthor = event.jda.shardManager?.retrieveUserById(msg.authorId)?.await() ?: return
        val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, messageAuthor.idLong)
        eb.setColor(Color.decode("#000001"))

        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val footer = i18n.getTranslation(language, "listener.message.deletion.log.footer")
            .replace(PLACEHOLDER_USER, messageAuthor.asTag)
        eb.setFooter(footer, messageAuthor.effectiveAvatarUrl)

        sendEmbed(container.daoManager.embedDisabledWrapper, sdmLogChannel, eb.build())
    }

    private suspend fun postDeletedByOtherLog(odmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent, deleterMember: Member) {
        if (odmLogChannel == null) return
        val messageAuthor = event.jda.shardManager?.retrieveUserById(msg.authorId)?.await() ?: return

        val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, deleterMember.idLong)
        eb.setColor(Color.decode("#000001"))

        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val footer = i18n.getTranslation(language, "listener.message.deletion.log.footer")
            .replace(PLACEHOLDER_USER, deleterMember.asTag)
        eb.setFooter(footer, deleterMember.user.effectiveAvatarUrl)

        sendEmbed(container.daoManager.embedDisabledWrapper, odmLogChannel, eb.build())

    }

    private suspend fun postDeletedByFilterLog(fmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent, cause: String?) {
        if (fmLogChannel == null) return
        val messageAuthor = event.jda.shardManager?.retrieveUserById(msg.authorId)?.await() ?: return

        val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, event.jda.selfUser.idLong)
        eb.setColor(Color.YELLOW)

        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val fieldTitle = i18n.getTranslation(language, "detected") + ":"
        eb.addField(fieldTitle, cause, false)

        val footer = i18n.getTranslation(language, "listener.message.deletion.log.footer")
            .replace(PLACEHOLDER_USER, event.jda.selfUser.asTag)
        eb.setFooter(footer, event.jda.selfUser.effectiveAvatarUrl)

        sendEmbed(container.daoManager.embedDisabledWrapper, fmLogChannel, eb.build())
    }

    private suspend fun getGeneralEmbedBuilder(
        msg: DaoMessage,
        event: GuildMessageDeleteEvent,
        messageAuthor: User,
        messageDeleterId: Long
    ): EmbedBuilder {
        val embedBuilder = EmbedBuilder()
        val channel = event.guild.getTextChannelById(msg.textChannelId)

        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val title =  i18n.getTranslation(language, "listener.message.deletion.log.title")
            .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "<#${msg.textChannelId}>")

        val extra = if (msg.authorId == messageDeleterId) ".self" else ""
        val description = i18n.getTranslation(language, "listener.message.deletion.log${extra}.description")
            .replace("%messageAuthor%", messageAuthor.asTag)
            .replace("%messageContent%", msg.content.replace("`", "´"))
            .replace("%messageAuthorId%", msg.authorId.toString())
            .replace("%messageDeleterId%", messageDeleterId.toString())
            .replace("%sentTime%", msg.moment.asEpochMillisToDateTime())
            .replace("%deletedTime%", System.currentTimeMillis().asEpochMillisToDateTime())

        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)
        embedBuilder.setThumbnail(messageAuthor.effectiveAvatarUrl)
        return embedBuilder
    }
}