package me.melijn.melijnbot.internals.events.eventlisteners

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.games.PokerCommand
import me.melijn.melijnbot.commands.games.RockPaperScissorsGame
import me.melijn.melijnbot.commands.utility.HelpCommand
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.SelfRoleUtil
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.music.DONATE_QUEUE_LIMIT
import me.melijn.melijnbot.internals.music.MusicPlayerManager
import me.melijn.melijnbot.internals.music.QUEUE_LIMIT
import me.melijn.melijnbot.internals.music.TrackUserData
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.Pagination
import me.melijn.melijnbot.internals.utils.message.StoringPagination
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent
import java.awt.Color
import java.lang.Integer.max
import java.lang.Integer.min

class MessageReactionAddedListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReactionAddEvent) onGuildMessageReactionAdd(event)
        if (event is PrivateMessageReactionAddEvent) {
            onPrivateMessageReactionAdd(event)
        }
        if (event is ButtonClickEvent) {
            onButtonClick(event)
        }
    }

    private fun onButtonClick(event: ButtonClickEvent) = TaskManager.async(event.channel) {
        searchMenuHandler(event)
        commandListHandler(event)
        paginationHandler(event)
    }

    private suspend fun paginationHandler(event: ButtonClickEvent) {
        val button = event.button ?: return
        val entry = container.paginationMap.entries.firstOrNull { (_, info) ->
            info.messageId == event.messageIdLong && info.authorId == event.user.idLong
        }
        if (entry != null) {
            val (_, info) = entry
            val pagination = info.toPagination()

            val message = pagination.navigate(button) ?: return
            info.currentPage = pagination.currentPage
            event.editMessage(message).queue()
        }

        val modularEntry = container.modularPaginationMap.entries.firstOrNull { (_, info) ->
            info.messageId == event.messageIdLong && info.authorId == event.user.idLong
        }
        if (modularEntry != null) {
            val (_, info) = modularEntry
            val pagination = info.toPagination()

            val message = pagination.navigate(button) ?: return
            info.currentPage = pagination.currentPage
            event.editMessage(message).queue()
        }
    }

    private fun commandListHandler(event: ButtonClickEvent) {
        val textChannel = if (event.isFromGuild) event.textChannel else null
        if (event.button?.id == "help_list")
            event.deferReply(true)
                .addEmbeds(
                    HelpCommand.ListArg.getHelpListMessage(container, textChannel, event.user, null).build()
                )
                .queue()
    }

    private fun onPrivateMessageReactionAdd(event: PrivateMessageReactionAddEvent) = TaskManager.async(event.channel) {
        paginationHandler(event)
        handleRPSReaction(event)
    }

    private suspend fun handleRPSReaction(event: PrivateMessageReactionAddEvent) {
        val author = event.channel.user
        if (event.userIdLong != author.idLong) return

        val rpsWrapper = container.daoManager.rpsWrapper
        val game = rpsWrapper.getGame(author.idLong) ?: return
        val choice = RockPaperScissorsGame.RPS.fromEmote(event.reactionEmote.emoji)

        if (game.user1 == author.idLong) {
            game.choice1 = choice
        } else {
            game.choice2 = choice
        }

        rpsWrapper.addGame(game)
    }

    // TODO fix duplicate code in this file
    private suspend fun paginationHandler(event: PrivateMessageReactionAddEvent) {
        val user = event.user ?: return
        if (event.reactionEmote.isEmote || user.isBot) return
        val emoji = event.reactionEmote.emoji
        if (!listOf("⏪", "◀️", "▶️", "⏩").contains(emoji)) return
        val entry = container.paginationMap.entries.firstOrNull { (_, info) ->
            info.messageId == event.messageIdLong
        }
        val modularEntry = container.modularPaginationMap.entries.firstOrNull { (_, info) ->
            info.messageId == event.messageIdLong
        }

        if (entry != null) {
            val pagination = entry.value

            val channel = event.channel
            val message = channel.retrieveMessageById(pagination.messageId).await()
            val newIndex = min(
                pagination.storage.size - 1, max(
                    0, when (emoji) {
                        "⏪" -> 0
                        "⏩" -> pagination.storage.size - 1
                        "◀️" -> pagination.currentPage - 1
                        "▶️" -> pagination.currentPage + 1
                        else -> return
                    }
                )
            )

            if (newIndex != pagination.storage.size)
                message.editMessage(pagination.storage[newIndex]).queue()

            pagination.currentPage = newIndex
            container.paginationMap[entry.key] = pagination

            val time = System.nanoTime()
            if (time.minus(lastCheck) > 60_000_000_000) {
                for (i in ArrayList(container.paginationMap.keys)) {
                    if (i < time - 3_600_000_000_000) {
                        container.paginationMap.remove(i)
                    }
                }
                lastCheck = time
            }
        } else if (modularEntry != null) {
            val pagination = modularEntry.value

            val channel = event.channel
            val message = channel.retrieveMessageById(pagination.messageId).await()
            val newIndex = min(
                pagination.storage.size - 1, max(
                    0, when (emoji) {
                        "⏪" -> 0
                        "⏩" -> pagination.storage.size - 1
                        "◀️" -> pagination.currentPage - 1
                        "▶️" -> pagination.currentPage + 1
                        else -> return
                    }
                )
            )

            if (newIndex != pagination.currentPage) {
                val msg = pagination.storage[newIndex].toMessage()
                    ?: throw IllegalArgumentException("cannot transform message")
                message.editMessage(msg).queue()
            }

            pagination.currentPage = newIndex
            container.modularPaginationMap[modularEntry.key] = pagination

            val time = System.nanoTime()
            if (time.minus(lastCheck) > 60_000_000_000) {
                for (i in ArrayList(container.modularPaginationMap.keys)) {
                    if (i < time - 3_600_000_000_000) {
                        container.modularPaginationMap.remove(i)
                    }
                }
                lastCheck = time
            }
        }
    }

    private fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) =
        TaskManager.async(event.user, event.channel) {
            selfRoleHandler(event)
            postReactionAddedLog(event)
            verificationHandler(event)
            paginationHandler(event)
            pokerHandler(event)
            starboardHandler(event)
        }

    private suspend fun starboardHandler(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot || event.reactionEmote.isEmote) return
        val emoji = event.reactionEmote.emoji
        if (emoji != "⭐") return

        val starboardSettings = container.daoManager.starboardSettingsWrapper
        val starboardMessageWrapper = container.daoManager.starboardMessageWrapper
        val sbChannel = event.guild.getAndVerifyChannelByType(
            container.daoManager,
            ChannelType.STARBOARD,
            Permission.MESSAGE_WRITE,
            Permission.MESSAGE_EMBED_LINKS
        ) ?: return

        val msg = starboardMessageWrapper.getStarboardInfo(event.messageIdLong)
        if (msg == null) {
            if (event.channel.isNSFW) return
            val reactions =
                event.channel.retrieveReactionUsersById(event.messageIdLong, "⭐").await().filter { !it.isBot }.size
            val ogMessage = event.channel.retrieveMessageById(event.messageIdLong).await()
            val settings = starboardSettings.getStarboardSettings(event.guild.idLong)
            if (reactions >= settings.minStars) {
                val starboardMessage =
                    getSendableStarboardMessage(
                        event,
                        reactions,
                        ogMessage.author,
                        ogMessage,
                        event.channel.idLong,
                        true
                    )
                val sendAction = sbChannel.sendMessage(starboardMessage)
                if (ogMessage.attachments.isNotEmpty()) {
                    ogMessage.attachments.firstOrNull { it.isVideo }?.let { video ->
                        sendAction.addFile(
                            video.retrieveInputStream().await(),
                            video.fileName + "." + video.fileExtension
                        )
                    }
                }
                val message = sendAction.await()
                if (ogMessage.guild.selfMember.hasPermission(sbChannel, Permission.MESSAGE_ADD_REACTION)) {
                    message.addReaction("⭐").await()
                }
                if (starboardMessageWrapper.getStarboardInfo(event.messageIdLong) != null) {
                    message.delete().reason("Fix race condition starboard").queue()
                }
                starboardMessageWrapper.setStarboardInfo(
                    event.guild.idLong, event.channel.idLong, ogMessage.author.idLong, event.messageIdLong,
                    message.idLong, reactions, false, System.currentTimeMillis()
                )
            }

        } else {
            if (msg.deleted) return
            val sbReactionUsers = sbChannel.retrieveReactionUsersById(msg.starboardMessageId, "⭐")
                .awaitOrNull()
                ?.filter { !it.isBot }
                ?.map { it.idLong } ?: return

            val partStars1 = sbReactionUsers.size
            val ogChannel = event.guild.getTextChannelById(msg.ogChannelId)
            val partStars2N = ogChannel
                ?.retrieveReactionUsersById(msg.ogMessageId, "⭐")
                ?.awaitOrNull()
                ?.filter { !it.isBot && !sbReactionUsers.contains(it.idLong) }
                ?.size

            val partStars2 = partStars2N ?: 0
            val newStarCount = partStars1 + partStars2
            if (newStarCount != msg.stars) {
                val message = sbChannel.retrieveMessageById(msg.starboardMessageId).await()
                val author = event.jda.shardManager?.retrieveUserById(msg.authorId)?.awaitOrNull()
                val newContent =
                    getSendableStarboardMessage(event, newStarCount, author, message, msg.ogChannelId, false)
                starboardMessageWrapper.setStarboardInfo(
                    event.guild.idLong,
                    msg.ogChannelId,
                    msg.authorId,
                    msg.starboardMessageId,
                    msg.starboardMessageId,
                    newStarCount,
                    msg.deleted,
                    msg.moment
                )
                message.editMessage(newContent).queue()
            }
        }
    }

    private fun getSendableStarboardMessage(
        event: GuildMessageReactionAddEvent,
        stars: Int,
        author: User?,
        message: Message,
        ogChannelId: Long,
        append: Boolean
    ): Message {
        val eb = Embedder(
            container.daoManager, event.guild.idLong, author?.idLong
                ?: -1
        ).setAuthor(author?.asTag ?: "deleted_user#0000", null, author?.effectiveAvatarUrl)

        if (message.embeds.size > 0) {
            val embed = message.embeds[0]
            eb.setTitle(embed.title, embed.url)
                .setDescription(embed.description)
                .setColor(embed.color)
                .setImage(embed.image?.url)
                .setThumbnail(embed.thumbnail?.url)
                .setTimestamp(embed.timestamp)
                .setFooter(embed.footer?.text, embed.footer?.iconUrl)

            for (field in embed.fields) {
                eb.addField(field.name, field.value, field.isInline)
            }
        } else {
            eb.setDescription(message.contentRaw.replace("[", "\\["))
        }
        if (append) eb.appendDescription("\n[`jump`](${message.jumpUrl})")

        for (attachment in message.attachments) {
            if (attachment.isImage) {
                eb.setImage(attachment.url)
            }
        }

        val messageBuilder = MessageBuilder()
            .setContent("`$stars⭐` message by ${author?.asMention ?: "`deleted_user#0000`"} in <#$ogChannelId>")
            .setEmbeds(eb.build())

        return messageBuilder.build()
    }

    private fun pokerHandler(event: GuildMessageReactionAddEvent) {
        if (event.reactionEmote.isEmote || event.user.isBot) return

        val game = PokerCommand.ongoingPoker.firstOrNull { game ->
            game.userId == event.userIdLong && game.msgId == event.messageIdLong
        } ?: return

        game.reactions = game.reactions + when (event.reactionEmote.emoji) {
            "1️⃣" -> 1
            "2️⃣" -> 2
            "3️⃣" -> 3
            "4️⃣" -> 4
            "5️⃣" -> 5

            else -> return
        }
    }

    var lastCheck = System.nanoTime()
    private suspend fun paginationHandler(event: GuildMessageReactionAddEvent) {
        if (event.reactionEmote.isEmote || event.user.isBot) return
        val emoji = event.reactionEmote.emoji
        if (!listOf("⏪", "◀️", "▶️", "⏩").contains(emoji)) return

        val entry = container.paginationMap.entries.firstOrNull { (_, info) ->
            info.messageId == event.messageIdLong && info.authorId == event.userIdLong
        }

        val modularEntry = container.modularPaginationMap.entries.firstOrNull { (_, info) ->
            info.messageId == event.messageIdLong && info.authorId == event.userIdLong
        }

        if (entry != null) {
            val pagination = entry.value

            val channel = event.channel
            val message = channel.retrieveMessageById(pagination.messageId).await()
            val newIndex = min(
                pagination.storage.size - 1, max(
                    0, when (emoji) {
                        "⏪" -> 0
                        "⏩" -> pagination.storage.size - 1
                        "◀️" -> pagination.currentPage - 1
                        "▶️" -> pagination.currentPage + 1
                        else -> return
                    }
                )
            )

            if (newIndex != pagination.storage.size)
                message.editMessage(pagination.storage[newIndex]).queue()

            pagination.currentPage = newIndex
            container.paginationMap[entry.key] = pagination

            if (event.guild.selfMember.hasPermission(event.channel, Permission.MESSAGE_MANAGE))
                event.reaction.removeReaction(event.user).queue()

            val time = System.nanoTime()
            if (time.minus(lastCheck) > 60_000_000_000) {
                for (i in ArrayList(container.paginationMap.keys)) {
                    if (i < time - 3_600_000_000_000) {
                        container.paginationMap.remove(i)
                    }
                }
                lastCheck = time
            }
        } else if (modularEntry != null) {
            val pagination = modularEntry.value

            val channel = event.channel
            val message = channel.retrieveMessageById(pagination.messageId).await()
            val newIndex = min(
                pagination.storage.size - 1, max(
                    0, when (emoji) {
                        "⏪" -> 0
                        "⏩" -> pagination.storage.size - 1
                        "◀️" -> pagination.currentPage - 1
                        "▶️" -> pagination.currentPage + 1
                        else -> return
                    }
                )
            )

            if (newIndex != pagination.currentPage) {
                val msg = pagination.storage[newIndex].toMessage()
                    ?: throw IllegalArgumentException("cannot transform message")
                message.editMessage(msg).queue()
            }

            pagination.currentPage = newIndex
            container.modularPaginationMap[modularEntry.key] = pagination

            if (event.guild.selfMember.hasPermission(event.channel, Permission.MESSAGE_MANAGE))
                event.reaction.removeReaction(event.user).queue()

            val time = System.nanoTime()
            if (time.minus(lastCheck) > 60_000_000_000) {
                for (i in ArrayList(container.modularPaginationMap.keys)) {
                    if (i < time - 3_600_000_000_000) {
                        container.modularPaginationMap.remove(i)
                    }
                }
                lastCheck = time
            }

        }
    }

    private suspend fun searchMenuHandler(event: ButtonClickEvent) {
        if (event.user.isBot) return
        val guild = event.guild ?: return
        val guildPlayer = MusicPlayerManager.guildMusicPlayers[guild.idLong] ?: return
        val menus = guildPlayer.searchMenus
        val menu = menus.getOrElse(event.messageIdLong) {
            return
        }

        val tracks = menu.audioTracks

        if (event.user.idLong != (tracks.first().userData as TrackUserData).userId) return

        val track: AudioTrack? = when (event.componentId) {
            "1" -> tracks.getOrElse(0) { return }
            "2" -> tracks.getOrElse(1) { return }
            "3" -> tracks.getOrElse(2) { return }
            "4" -> tracks.getOrElse(3) { return }
            "5" -> tracks.getOrElse(4) { return }
            "cancel" -> null
            else -> return
        }

        guildPlayer.searchMenus.remove(event.messageIdLong)
        val language = getLanguage(container.daoManager, event.user.idLong, guild.idLong)
        when {
            track == null -> {
                val title = i18n.getTranslation(language, "message.music.searchmenu")
                val desc = i18n.getTranslation(language, "message.music.search.cancelled.description")
                val eb = Embedder(container.daoManager, guild.idLong, event.user.idLong)
                    .setTitle(title)
                    .setDescription(desc)
                event.editMessage(MessageBuilder().setEmbeds(eb.build()).build()).queue()
            }
            guildPlayer.safeQueueSilent(container.daoManager, track, menu.nextPosition) -> {
                guild.selfMember.voiceState?.channel?.let {
                    LogUtils.addMusicPlayerNewTrack(container.daoManager, container.lavaManager, it, event.user, track)
                }

                val title = i18n.getTranslation(language, "message.music.addedtrack.title")
                    .withVariable(PLACEHOLDER_USER, event.user.asTag)

                val description = i18n.getTranslation(language, "message.music.addedtrack.description")
                    .withVariable("position", guildPlayer.guildTrackManager.getPosition(track).toString())
                    .withVariable("title", track.info.title)
                    .withVariable("duration", getDurationString(track.duration))
                    .withVariable("url", track.info.uri)

                val eb = Embedder(container.daoManager, guild.idLong, event.user.idLong)
                eb.setTitle(title)
                eb.setDescription(description)
                event.editMessage(MessageBuilder().setEmbeds(eb.build()).build()).queue()
            }
            else -> {
                val msg = i18n.getTranslation(language, "message.music.queuelimit")
                    .withVariable("amount", QUEUE_LIMIT.toString())
                    .withVariable("donateAmount", DONATE_QUEUE_LIMIT.toString())
                event.editMessage(MessageBuilder().setContent(msg).build()).queue()
            }
        }
    }

    private suspend fun verificationHandler(event: GuildMessageReactionAddEvent) {
        val textChannel = event.channel
        val guild = event.guild
        val member = event.member
        val dao = container.daoManager

        val verificationChannel =
            guild.getAndVerifyChannelByType(dao, ChannelType.VERIFICATION, Permission.MESSAGE_MANAGE)
                ?: return
        if (verificationChannel.idLong != textChannel.idLong) return
        val verificationType = dao.verificationTypeWrapper.getType(guild.idLong)
        if (verificationType != VerificationType.REACTION) return

        val unverifiedRole = VerificationUtils.getUnverifiedRoleN(event.channel, dao) ?: return
        if (!dao.unverifiedUsersWrapper.contains(
                guild.idLong,
                member.idLong
            ) && !member.roles.contains(unverifiedRole)
        ) {
            //User is already verified
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                //User doesn't have admin perms to add reaction in verification channel
                event.reaction.removeReaction(event.user).queue()
            }
            return
        }

        verificationType.let {
            when (it) {
                VerificationType.REACTION -> {
                    val code = dao.verificationEmotejiWrapper.getEmoteji(guild.idLong)
                    if (
                        (event.reactionEmote.isEmoji && event.reactionEmote.emoji == code) ||
                        (event.reactionEmote.isEmote && event.reactionEmote.emote.id == code)
                    ) {
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
        }

        event.reaction.removeReaction(event.user).queue()
    }

    private suspend fun postReactionAddedLog(event: GuildMessageReactionAddEvent) {
        val dao = container.daoManager

        val logChannel = event.guild.getAndVerifyLogChannelByType(dao, LogChannelType.REACTION)
            ?: return

        val botLogState = dao.botLogStateWrapper.shouldLog(event.guild.idLong)
        if (!botLogState && event.member.user.isBot) return

        val zoneId = getZoneId(dao, event.guild.idLong)

        val language = getLanguage(dao, -1, event.guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.reaction.log.title")
            .withVariable(PLACEHOLDER_CHANNEL, event.channel.asTag)

        val isEmote = event.reactionEmote.isEmote
        val part = if (isEmote) "emote" else "emoji"
        val description = i18n.getTranslation(language, "listener.message.reaction.$part.log.description")
            .withVariable(PLACEHOLDER_USER_ID, event.member.id)
            .withVariable("messageId", event.messageId)
            .withVariable("emoteName", event.reactionEmote.name)
            .withVariable("emoteId", if (isEmote) event.reactionEmote.id else "/")
            .withVariable("moment", System.currentTimeMillis().asEpochMillisToDateTime(zoneId))
            .withVariable(
                "messageUrl",
                "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.messageId}"
            )
            .withVariable("emoteUrl", if (isEmote) event.reactionEmote.emote.imageUrl else "/")

        val footer = i18n.getTranslation(language, "listener.message.reaction.log.footer")
            .withVariable(PLACEHOLDER_USER, event.member.asTag)

        val embedBuilder = EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setThumbnail(if (isEmote) event.reactionEmote.emote.imageUrl else null)
            .setFooter(footer, event.member.user.effectiveAvatarUrl)
            .setColor(Color.WHITE)

        sendEmbed(dao.embedDisabledWrapper, logChannel, embedBuilder.build())
    }

    private suspend fun selfRoleHandler(event: GuildMessageReactionAddEvent) {
        if (event.user.isBot) return

        val guild = event.guild
        val member = event.member
        val roles = SelfRoleUtil.getSelectedSelfRoleNByReactionEvent(event, container) ?: return

        for (role in roles) {
            val added = try {
                guild.addRoleToMember(member, role).reason("SelfRole").awaitBool()
            } catch (t: Throwable) {
                false
            }
            if (!added) {
                LogUtils.sendMessageFailedToAddRoleToMember(container.daoManager, member, role)
            }
        }
    }
}

private fun StoragePaginationInfo.toPagination(): Pagination {
    return StoringPagination(this.storage.map { ModularMessage(it) }, currentPage)
}
private fun ModularStoragePaginationInfo.toPagination(): Pagination {
    return StoringPagination(this.storage.map { it }, currentPage)
}
