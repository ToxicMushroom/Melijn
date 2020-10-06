package me.melijn.melijnbot.internals.events.eventlisteners

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.utility.HelpCommand
import me.melijn.melijnbot.database.message.DaoMessage
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.FilterUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import me.melijn.melijnbot.internals.utils.message.toMessage
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import java.awt.Color

class MessageReceivedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReceivedEvent) {
            TaskManager.async(event.author, event.channel) {
                handleMessageReceivedStoring(event)
                handleAttachmentLog(event)
                handleVerification(event)
                FilterUtil.handleFilter(container, event.message)
                // SpammingUtil.handleSpam(container, event.message)
            }
        }
        if (event is MessageReceivedEvent) {
            TaskManager.async(event.author, event.channel) {
                handleSimpleMelijnPing(event)
            }
        }
    }

    private suspend fun handleSimpleMelijnPing(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val tags = arrayOf("<@${event.jda.selfUser.idLong}>", "<@!${event.jda.selfUser.idLong}>")
        val usedMention = event.message.contentRaw.trim()
        if (!tags.contains(usedMention)) return

        val helpCmd = container.commandMap.values.firstOrNull { cmd ->
            cmd is HelpCommand
        } ?: return


        if (event.isFromGuild && !event.textChannel.canTalk()) {
            try {
                val pChannel = event.author.openPrivateChannel().awaitOrNull()
                val language = getLanguage(container.daoManager, event.author.idLong)
                val msg = i18n.getTranslation(language, "message.melijnping.nowriteperms")
                    .withVariable("server", event.guild.name)
                    .withVariable(PLACEHOLDER_CHANNEL, event.textChannel.asMention)

                pChannel?.sendMessage(msg)?.queue({}, {})
            } catch (t: Throwable) {
                t.printStackTrace()
            }
            return
        }

        val cmdContext = CommandContext(event, listOf(usedMention, "help"), container, container.commandMap.values.toSet(),
            mutableMapOf(), mutableMapOf(), true, "${usedMention}help")
        helpCmd.run(cmdContext)
    }

    private suspend fun handleVerification(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        if (!event.message.isFromGuild) return
        val textChannel = event.channel
        val guild = event.guild
        val member = event.member ?: return
        val dao = container.daoManager

        val verificationChannel = guild.getAndVerifyChannelByType(dao, ChannelType.VERIFICATION, Permission.MESSAGE_MANAGE)
            ?: return
        if (verificationChannel.idLong != textChannel.idLong) return


        val unverifiedRole = VerificationUtils.getUnverifiedRoleN(event.channel, dao) ?: return
        if (!dao.unverifiedUsersWrapper.contains(guild.idLong, member.idLong) && !member.roles.contains(unverifiedRole)) {
            //User is already verified
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                container.botDeletedMessageIds.add(event.messageIdLong)
                //User doesn't have admin perms to send message in verification channel
                event.message.delete().reason("verification channel").queue({}, {})
            }
            return
        }

        when (dao.verificationTypeWrapper.getType(guild.idLong)) {
            VerificationType.PASSWORD -> {
                val password = dao.verificationPasswordWrapper.getPassword(guild.idLong)
                if (event.message.contentRaw == password) {
                    VerificationUtils.verify(dao, container.webManager.proxiedHttpClient,
                        unverifiedRole, guild.selfMember.user, member)
                } else {
                    VerificationUtils.failedVerification(dao, member)
                }
            }

            VerificationType.GOOGLE_RECAPTCHAV2 -> {
                val code = dao.unverifiedUsersWrapper.getMoment(guild.idLong, member.idLong)
                if (event.message.contentRaw == code.toString()) {
                    VerificationUtils.verify(dao, container.webManager.proxiedHttpClient,
                        unverifiedRole, guild.selfMember.user, member)
                } else {
                    VerificationUtils.failedVerification(dao, member)
                }
            }
            else -> {
            }
        }

        container.botDeletedMessageIds.add(event.messageIdLong)
        event.message.delete().reason("verification channel").queue({}, {})
    }


    private suspend fun handleAttachmentLog(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        if (event.message.attachments.isEmpty()) return
        val channel = event.guild.getAndVerifyLogChannelByType(container.daoManager, LogChannelType.ATTACHMENT, true)
            ?: return

        for (attachment in event.message.attachments) {
            postAttachmentLog(event, channel, attachment)
        }
    }

    suspend fun shouldLogBots(guildId: Long): Boolean {
        return container.daoManager.supporterWrapper.getGuilds().contains(guildId) &&
            container.daoManager.botLogStateWrapper.shouldLog(guildId)
    }

    private suspend fun handleMessageReceivedStoring(event: GuildMessageReceivedEvent) {
        if (event.author.isBot && event.author.idLong != container.settings.botInfo.id && shouldLogBots(event.guild.idLong)) return
        val guildId = event.guild.idLong
        val daoManager = container.daoManager
        val logChannelWrapper = daoManager.logChannelWrapper

        val odmId = GlobalScope.async { logChannelWrapper.getChannelId(guildId, LogChannelType.OTHER_DELETED_MESSAGE) }
        val sdmId = GlobalScope.async { logChannelWrapper.getChannelId(guildId, LogChannelType.SELF_DELETED_MESSAGE) }
        val pmId = GlobalScope.async { logChannelWrapper.getChannelId(guildId, LogChannelType.PURGED_MESSAGE) }
        val fmId = GlobalScope.async { logChannelWrapper.getChannelId(guildId, LogChannelType.FILTERED_MESSAGE) }
        val emId = GlobalScope.async { logChannelWrapper.getChannelId(guildId, LogChannelType.EDITED_MESSAGE) }
        if (odmId.await() == -1L && sdmId.await() == -1L && pmId.await() == -1L && fmId.await() == -1L && emId.await() == -1L) return


        val messageWrapper = daoManager.messageHistoryWrapper
        var content = event.message.contentRaw
        event.message.embeds.forEach { embed ->
            content += "\n${embed.toMessage()}"
        }

        TaskManager.async(event.author, event.channel) {
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
        val guild = event.guild
        val daoManager = container.daoManager
        val channel = event.channel

        val language = getLanguage(daoManager, -1, guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.attachment.log.title")
            .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)

        val author = event.author
        val message = event.message
        val description = i18n.getTranslation(language, "listener.message.attachment.log.description")
            .withVariable(PLACEHOLDER_USER_ID, author.id)
            .withVariable("messageId", event.messageId)
            .withVariable("messageUrl", "https://discordapp.com/channels/${guild.id}/${channel.id}/${message.id}")
            .withVariable("attachmentUrl", attachment.url)
            .withVariable("moment", message.timeCreated.asLongLongGMTString())

        val footer = i18n.getTranslation(language, "listener.message.attachment.log.footer")
            .withVariable(PLACEHOLDER_USER, author.asTag)

        val embedBuilder = EmbedBuilder()
            .setFooter(footer, author.effectiveAvatarUrl)
            .setColor(Color(0xDC143C))
            .setImage(attachment.url)
            .setTitle(title)
            .setDescription(description)

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, embedBuilder.build())
    }
}