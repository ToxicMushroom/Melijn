package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.FilterUtil
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color

class MessageReceivedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) = runBlocking {
        if (event is GuildMessageReceivedEvent) {
            handleMessageReceivedStoring(event)
            handleAttachmentLog(event)
            handleVerification(event)
            handleFilter(event)
        }
    }

    private suspend fun handleFilter(event: GuildMessageReceivedEvent) = container.taskManager.async {
        val guildId = event.guild.idLong
        val channelId = event.channel.idLong
        val member = event.member ?: return@async
        val daoManager = container.daoManager
        if (member.hasPermission(event.channel, Permission.MESSAGE_MANAGE) || member == event.guild.selfMember) return@async

        val channelFilterMode = daoManager.filterModeWrapper.filterWrappingModeCache.get(Pair(guildId, channelId)).await()
        val effectiveMode: FilterMode = if (channelFilterMode == FilterMode.NO_MODE) {
            val guildFilterMode = daoManager.filterModeWrapper.filterWrappingModeCache.get(Pair(guildId, null)).await()
            if (guildFilterMode == FilterMode.NO_MODE) {
                FilterMode.DEFAULT
            } else {
                guildFilterMode
            }
        } else channelFilterMode

        when (effectiveMode) {
            FilterMode.MUST_MATCH_ALLOWED_FORMAT -> {

            }
            FilterMode.MUST_MATCH_ALLOWED_FORMAT_EXCLUDE_FILTER -> {

            }
            FilterMode.NO_WRAP -> {

            }
            FilterMode.DEFAULT -> {
                FilterUtil.filterDefault(container, event.message)
            }
            FilterMode.NO_MODE -> throw IllegalArgumentException("Should be DEFAULT rn")
            FilterMode.DISABLED -> return@async
        }
    }

    private suspend fun handleVerification(event: GuildMessageReceivedEvent) {
        if (!event.message.isFromGuild) return
        val textChannel = event.channel
        val guild = event.guild
        val member = event.member ?: return
        val dao = container.daoManager

        val verificationChannel = guild.getAndVerifyChannelByType(ChannelType.VERIFICATION, dao, Permission.MESSAGE_MANAGE)
            ?: return
        if (verificationChannel.idLong != textChannel.idLong) return

        val unverifiedRole = VerificationUtils.getUnverifiedRoleN(event.channel, dao) ?: return
        if (!dao.unverifiedUsersWrapper.contains(guild.idLong, member.idLong) && !member.roles.contains(unverifiedRole)) {
            //User is already verified
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                //User doesn't have admin perms to send message in verification channel
                event.message.delete().queue()
            }
            return
        }

        val verificationType = dao.verificationTypeWrapper.verificationTypeCache[guild.idLong].await()
        verificationType?.let {
            when (it) {
                VerificationType.CODE -> {
                    val code = dao.verificationCodeWrapper.verificationCodeCache[guild.idLong].await()
                    if (event.message.contentRaw == code) {
                        VerificationUtils.verify(dao, unverifiedRole, guild.selfMember.user, member)
                    } else {
                        VerificationUtils.failedVerification(dao, member)
                    }
                }

                VerificationType.GOOGLE_RECAPTCHAV2 -> {
                    val code = dao.unverifiedUsersWrapper.getMoment(guild.idLong, member.idLong)
                    if (event.message.contentRaw == code.toString()) {
                        VerificationUtils.verify(dao, unverifiedRole, guild.selfMember.user, member)
                    } else {
                        VerificationUtils.failedVerification(dao, member)
                    }

                }
                else -> {
                }
            }
        }

        event.message.delete().queue()
    }


    private fun handleAttachmentLog(event: GuildMessageReceivedEvent) {
        if (event.message.attachments.isEmpty()) return
        val logChannelWrapper = container.daoManager.logChannelWrapper
        val logChannelCache = logChannelWrapper.logChannelCache
        CoroutineScope(Dispatchers.Default).launch {
            val attachmentLogChannelId = logChannelCache.get(Pair(event.guild.idLong, LogChannelType.ATTACHMENT)).await()
            if (attachmentLogChannelId == -1L) return@launch
            val logChannel = event.guild.getTextChannelById(attachmentLogChannelId) ?: return@launch
            for (attachment in event.message.attachments) {
                postAttachmentLog(event, logChannel, attachment)
            }
        }
    }

    private fun handleMessageReceivedStoring(event: GuildMessageReceivedEvent) = runBlocking {
        val guildId = event.guild.idLong
        val logChannelWrapper = container.daoManager.logChannelWrapper
        val logChannelCache = logChannelWrapper.logChannelCache

        val odmId = logChannelCache.get(Pair(guildId, LogChannelType.OTHER_DELETED_MESSAGE))
        val sdmId = logChannelCache.get(Pair(guildId, LogChannelType.SELF_DELETED_MESSAGE))
        val pmId = logChannelCache.get(Pair(guildId, LogChannelType.PURGED_MESSAGE))
        val fmId = logChannelCache.get(Pair(guildId, LogChannelType.FILTERED_MESSAGE))
        if (odmId.await() == -1L && sdmId.await() == -1L && pmId.await() == -1L && fmId.await() == -1L) return@runBlocking

        val messageWrapper = container.daoManager.messageHistoryWrapper
        var content = event.message.contentRaw
        event.message.embeds.forEach { embed ->
            content += "\n${embed.toMessage()}"
        }

        GlobalScope.launch {
            messageWrapper.addMessage(DaoMessage(
                guildId,
                event.channel.idLong,
                event.author.idLong,
                event.messageIdLong,
                event.message.contentRaw,
                event.message.timeCreated.toInstant().toEpochMilli()
            ))
        }
    }

    private suspend fun postAttachmentLog(event: GuildMessageReceivedEvent, logChannel: TextChannel, attachment: Message.Attachment) {
        val embedBuilder = EmbedBuilder()
        val language = getLanguage(container.daoManager, -1, event.guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.attachment.log.title")
            .replace(PLACEHOLDER_CHANNEL, event.channel.asTag)

        val description = i18n.getTranslation(language, "listener.message.attachment.log.description")
            .replace(PLACEHOLDER_USER_ID, event.author.id)
            .replace("%messageId%", event.messageId)
            .replace("%messageUrl%", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.message.id}")
            .replace("%attachmentUrl%", attachment.url)
            .replace("%moment%", event.message.timeCreated.asLongLongGMTString())

        val footer = i18n.getTranslation(language, "listener.message.attachment.log.footer")
            .replace(PLACEHOLDER_USER, event.author.asTag)

        embedBuilder.setFooter(footer, event.author.effectiveAvatarUrl)

        embedBuilder.setColor(Color.decode("#DC143C"))
        embedBuilder.setImage(attachment.url)

        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)

        sendEmbed(container.daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
    }
}