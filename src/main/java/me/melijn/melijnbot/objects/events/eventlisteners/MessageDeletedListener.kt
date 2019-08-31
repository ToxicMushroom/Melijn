package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.translation.Translateable
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

    private fun onGuildMessageDelete(event: GuildMessageDeleteEvent) = runBlocking {
        val timeOne = System.currentTimeMillis()
        val guild = event.guild
        val guildId = event.guild.idLong
        val selfMember = guild.selfMember
        val logChannelWrapper = container.daoManager.logChannelWrapper
        val logChannelCache = logChannelWrapper.logChannelCache
        if (!guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) return@runBlocking

        val odmId = logChannelCache.get(Pair(guildId, LogChannelType.OTHER_DELETED_MESSAGE)).await()
        val sdmId = logChannelCache.get(Pair(guildId, LogChannelType.SELF_DELETED_MESSAGE)).await()
        val pmId = logChannelCache.get(Pair(guildId, LogChannelType.PURGED_MESSAGE)).await()
        val fmId = logChannelCache.get(Pair(guildId, LogChannelType.FILTERED_MESSAGE)).await()
        if (odmId == -1L && sdmId == -1L && pmId == -1L && fmId == -1L) return@runBlocking

        val odmLogChannel = if (odmId != -1L) {
            guild.getTextChannelById(odmId)
        } else {
            null
        }
        if (
                (odmLogChannel == null && odmId != -1L) ||
                (odmLogChannel != null && !selfMember.hasPermission(odmLogChannel, Permission.MESSAGE_WRITE))
        ) {
            logChannelWrapper.removeChannel(guildId, LogChannelType.OTHER_DELETED_MESSAGE)
        }

        val sdmLogChannel = if (sdmId != -1L) {
            guild.getTextChannelById(sdmId)
        } else {
            null
        }
        if (
                (sdmLogChannel == null && sdmId != -1L) ||
                (sdmLogChannel != null && !selfMember.hasPermission(sdmLogChannel, Permission.MESSAGE_WRITE))
        ) {
            logChannelWrapper.removeChannel(guildId, LogChannelType.SELF_DELETED_MESSAGE)
        }

        val pmLogChannel = if (pmId != -1L) {
            guild.getTextChannelById(pmId)
        } else {
            null
        }
        if (
                (pmLogChannel == null && pmId != -1L) ||
                (pmLogChannel != null && !selfMember.hasPermission(pmLogChannel, Permission.MESSAGE_WRITE))
        ) {
            logChannelWrapper.removeChannel(guildId, LogChannelType.PURGED_MESSAGE)
        }

        val fmLogChannel = if (fmId != -1L) {
            guild.getTextChannelById(fmId)
        } else {
            null
        }
        if (
                (fmLogChannel == null && fmId != -1L) ||
                (fmLogChannel != null && !selfMember.hasPermission(fmLogChannel, Permission.MESSAGE_WRITE))
        ) {
            logChannelWrapper.removeChannel(guildId, LogChannelType.FILTERED_MESSAGE)
        }

        if (odmLogChannel == null && sdmLogChannel == null && pmLogChannel == null && fmLogChannel == null) return@runBlocking

        CoroutineScope(Dispatchers.Default).launch {
            selectCorrectLogType(event, odmLogChannel, sdmLogChannel, pmLogChannel, fmLogChannel)
        }

//        GlobalScope.launch(Dispatchers.Default) {
//            selectCorrectLogType(event, odmLogChannel, sdmLogChannel, pmLogChannel, fmLogChannel)
//        }

        println("duration delete: " + (System.currentTimeMillis() - timeOne))
    }

    private suspend fun selectCorrectLogType(
            event: GuildMessageDeleteEvent,
            odmLogChannel: TextChannel?,
            sdmLogChannel: TextChannel?,
            pmLogChannel: TextChannel?,
            fmLogChannel: TextChannel?
    ) {
        val guild = event.guild
        val msg = container.daoManager.messageWrapper.getMessageById(event.messageIdLong) ?: return
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
                println("Bot filtered this message")
                return
            }
            container.botDeletedMessageIds.contains(msg.messageId) -> {
                postDeletedByOtherLog(odmLogChannel, msg, event, event.guild.selfMember)
                container.botDeletedMessageIds.remove(msg.messageId)
                println("Bot deleted this message")
                return
            }
            else -> guild.retrieveAuditLogs().type(ActionType.MESSAGE_DELETE).limit(50).queue { list ->
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
                    val user = entry.user ?: return@queue
                    val member = event.guild.getMember(user) ?: return@queue

                    postDeletedByOtherLog(odmLogChannel, msg, event, member)
                    println("Other deleted the user's message ${entry.getOption<String>(AuditLogOption.COUNT)}")
                } else {
                    postDeletedBySelfLog(sdmLogChannel, msg, event)
                    println("User deleted his own message")
                }
            }
        }
    }

    private fun postDeletedByPurgeLog(pmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent, purgeRequesterId: Long) {
        if (pmLogChannel == null) return
        event.jda.shardManager?.retrieveUserById(msg.authorId)?.queue { messageAuthor ->
            val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, event.jda.selfUser.idLong)
            eb.setColor(Color.decode("#551A8B"))

            sendEmbed(container.daoManager.embedDisabledWrapper, pmLogChannel, eb.build())
        }
    }

    private fun postDeletedBySelfLog(sdmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent) {
        if (sdmLogChannel == null) return
        event.jda.shardManager?.retrieveUserById(msg.authorId)?.queue { messageAuthor ->
            val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, messageAuthor.idLong)
            eb.setColor(Color.decode("#000001"))

            sendEmbed(container.daoManager.embedDisabledWrapper, sdmLogChannel, eb.build())
        }
    }

    private fun postDeletedByOtherLog(odmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent, member: Member) = runBlocking {
        if (odmLogChannel == null) return@runBlocking
        val messageAuthor = event.jda.shardManager?.retrieveUserById(msg.authorId)?.await() ?: return@runBlocking

        val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, member.idLong)
        eb.setColor(Color.decode("#000001"))

        sendEmbed(container.daoManager.embedDisabledWrapper, odmLogChannel, eb.build())

    }

    private fun postDeletedByFilterLog(fmLogChannel: TextChannel?, msg: DaoMessage, event: GuildMessageDeleteEvent, cause: String?) {
        if (fmLogChannel == null) return
        event.jda.shardManager?.retrieveUserById(msg.authorId)?.queue { messageAuthor ->
            val eb = getGeneralEmbedBuilder(msg, event, messageAuthor, event.jda.selfUser.idLong)
            eb.setColor(Color.YELLOW)

            sendEmbed(container.daoManager.embedDisabledWrapper, fmLogChannel, eb.build())
        }
    }

    private fun getGeneralEmbedBuilder(
            msg: DaoMessage,
            event: GuildMessageDeleteEvent,
            messageAuthor: User,
            messageDeleterId: Long
    ): EmbedBuilder {
        val embedBuilder = EmbedBuilder()
        val channel = event.guild.getTextChannelById(msg.textChannelId)
        val title = Translateable("listener.message.deletion.log.title")
                .string(container.daoManager, event.guild.idLong)
                .replace("%channel%", channel?.asTag ?: "<#${msg.textChannelId}>")

        val extra = if (msg.authorId == messageDeleterId) ".self" else ""
        val description = Translateable("listener.message.deletion.log${extra}.description")
                .string(container.daoManager, event.guild.idLong)
                .replace("%messageAuthor%", messageAuthor.asTag)
                .replace("%messageContent%", msg.content)
                .replace("%messageAuthorId%", msg.authorId.toString())
                .replace("%messageDeleterId%", messageDeleterId.toString())
                .replace("%sentTime%", msg.moment.asEpochMillisToDateTime())
        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)
        embedBuilder.setThumbnail(messageAuthor.effectiveAvatarUrl)
        return embedBuilder
    }
}