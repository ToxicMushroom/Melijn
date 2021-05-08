package me.melijn.melijnbot.internals.events.eventlisteners

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.games.RockPaperScissorsGame
import me.melijn.melijnbot.commands.games.TicTacToeGame
import me.melijn.melijnbot.commands.utility.HelpCommand
import me.melijn.melijnbot.commandutil.game.TicTacToe
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
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import java.awt.Color

class MessageReceivedListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReceivedEvent) {
            TaskManager.async(event.author, event.channel) {
                handleMessageReceivedStoring(event)
                handleAttachmentLog(event)
                handleVerification(event)
                FilterUtil.handleFilter(container, event.message)
                handleRemoveInactive(container, event)
                // SpammingUtil.handleSpam(container, event.message)
            }
        } else if (event is PrivateMessageReceivedEvent) {
            TaskManager.async(event.author, event.channel) {
                checkTicTacToe(event)
                checkRockPaperScissors(event)
            }
        }
        if (event is MessageReceivedEvent) {
            TaskManager.async(event.author, event.channel) {
                handleSimpleMelijnPing(event)
            }
        }
    }

    private suspend fun handleRemoveInactive(container: Container, event: GuildMessageReceivedEvent) {
        val guildId = event.guild.idLong
        if (isPremiumGuild(container.daoManager, guildId)) {
            container.daoManager.inactiveJMWrapper.delete(guildId, event.author.idLong)
        }
    }


    private suspend fun checkRockPaperScissors(event: PrivateMessageReceivedEvent) {
        val author = event.author
        val rpsWrapper = container.daoManager.rpsWrapper
        val game = rpsWrapper.getGame(author.idLong) ?: return
        val choice = try {
            RockPaperScissorsGame.RPS.valueOf(event.message.contentRaw.uppercase())
        } catch (t: Throwable) {
            null
        }

        if (game.user1 == author.idLong) {
            game.choice1 = choice
        } else {
            game.choice2 = choice
        }

        rpsWrapper.addGame(game)
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

        val cmdContext = CommandContext(
            event.message, listOf(usedMention, "help"), container, container.commandMap.values.toSet(),
            mutableMapOf(), mutableMapOf(), true, "${usedMention}help"
        )
        helpCmd.run(cmdContext)
    }

    private suspend fun handleVerification(event: GuildMessageReceivedEvent) {
        if (event.author.isBot) return
        if (!event.message.isFromGuild) return
        val textChannel = event.channel
        val guild = event.guild
        val member = event.member ?: return
        val dao = container.daoManager

        val verificationChannel =
            guild.getAndVerifyChannelByType(dao, ChannelType.VERIFICATION, Permission.MESSAGE_MANAGE)
                ?: return
        if (verificationChannel.idLong != textChannel.idLong) return


        val unverifiedRole = VerificationUtils.getUnverifiedRoleN(event.channel, dao) ?: return
        if (!dao.unverifiedUsersWrapper.contains(
                guild.idLong,
                member.idLong
            ) && !member.roles.contains(unverifiedRole)
        ) {
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
                    VerificationUtils.verify(
                        dao, container.webManager.proxiedHttpClient,
                        unverifiedRole, guild.selfMember.user, member
                    )
                } else {
                    VerificationUtils.failedVerification(dao, member)
                }
            }

            VerificationType.GOOGLE_RECAPTCHAV2 -> {
                val code = dao.unverifiedUsersWrapper.getMoment(guild.idLong, member.idLong)
                if (event.message.contentRaw == code.toString()) {
                    VerificationUtils.verify(
                        dao, container.webManager.proxiedHttpClient,
                        unverifiedRole, guild.selfMember.user, member
                    )
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

    private suspend fun shouldLogBots(guildId: Long): Boolean {
        val daoManager = container.daoManager
        return isPremiumGuild(daoManager, guildId) &&
            daoManager.botLogStateWrapper.shouldLog(guildId)
    }

    private suspend fun handleMessageReceivedStoring(event: GuildMessageReceivedEvent) {
        if (event.author.isBot && event.author.idLong != container.settings.botInfo.id && !shouldLogBots(event.guild.idLong)) return
        val guildId = event.guild.idLong
        val daoManager = container.daoManager
        if (awaitAll(
                channelIdByTypeAsync(guildId, LogChannelType.OTHER_DELETED_MESSAGE),
                channelIdByTypeAsync(guildId, LogChannelType.SELF_DELETED_MESSAGE),
                channelIdByTypeAsync(guildId, LogChannelType.PURGED_MESSAGE),
                channelIdByTypeAsync(guildId, LogChannelType.FILTERED_MESSAGE),
                channelIdByTypeAsync(guildId, LogChannelType.EDITED_MESSAGE),
                channelIdByTypeAsync(guildId, LogChannelType.BULK_DELETED_MESSAGE)
            ).all {
                it == -1L
            }
        ) {
            return
        }

        val messageWrapper = daoManager.messageHistoryWrapper
        val content = event.message.contentRaw
        val embeds = event.message.embeds.joinToString("\n") { embed ->
            embed.toMessage()
        }
        val attachments = event.message.attachments.map { it.url }

        TaskManager.async(event.author, event.channel) {
            messageWrapper.addMessage(
                DaoMessage(
                    guildId,
                    event.channel.idLong,
                    event.author.idLong,
                    event.messageIdLong,
                    content,
                    embeds,
                    attachments,
                    event.message.timeCreated.toInstant().toEpochMilli()
                )
            )
        }
    }

    private suspend fun channelIdByTypeAsync(guildId: Long, channelType: LogChannelType): Deferred<Long> {
        return TaskManager.taskValueAsync {
            container.daoManager.logChannelWrapper.getChannelId(guildId, channelType)
        }
    }

    private suspend fun postAttachmentLog(
        event: GuildMessageReceivedEvent,
        logChannel: TextChannel,
        attachment: Message.Attachment
    ) {
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

    private suspend fun checkTicTacToe(event: PrivateMessageReceivedEvent) {
        val shardManager = event.jda.shardManager ?: return
        val author = event.author
        if (author.isBot) return

        val game = container.daoManager.tttWrapper.getGame(author.idLong) ?: return
        val tttState = if (author.idLong == game.user1 && TicTacToe.isTurnUserOne(game.gameState)) TicTacToeGame.TTTState.O
        else if (author.idLong == game.user2 && !TicTacToe.isTurnUserOne(game.gameState)) TicTacToeGame.TTTState.X
        else return

        parseFieldNMessage(event, game, tttState)?.let {
            game.gameState = it
            container.daoManager.tttWrapper.addGame(game)
            TicTacToe.sendNewMenu(shardManager, container.daoManager, game)
        }
    }

    private val coordinatePattern = Regex("([1-3])\\s*[,.\\-\\s]\\s*([1-3])")
    private fun parseFieldNMessage(
        event: PrivateMessageReceivedEvent,
        ttt1: TicTacToeGame,
        state: TicTacToeGame.TTTState
    ): Array<TicTacToeGame.TTTState>? {
        val content = event.message.contentRaw
        val out = coordinatePattern.matchEntire(content)
        if (out == null) {
            event.channel.sendMessage("That's not the right format :/. example: `1,1`").queue()
            return null
        }

        val x = out.groupValues[1].toInt()
        val y = out.groupValues[2].toInt()
        val linearIndex = (x - 1) + ((y - 1) * 3)
        val array = ttt1.gameState
        if (array[linearIndex] != TicTacToeGame.TTTState.EMPTY) {
            event.channel.sendMessage("That coordinate has been used already.").queue()
            return null
        }
        array[linearIndex] = state
        return array
    }
}