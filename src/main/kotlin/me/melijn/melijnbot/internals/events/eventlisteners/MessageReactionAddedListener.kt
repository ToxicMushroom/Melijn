package me.melijn.melijnbot.internals.events.eventlisteners

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.SelfRoleUtil
import me.melijn.melijnbot.internals.music.DONATE_QUEUE_LIMIT
import me.melijn.melijnbot.internals.music.QUEUE_LIMIT
import me.melijn.melijnbot.internals.music.TrackUserData
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.sendEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.priv.react.PrivateMessageReactionAddEvent
import java.awt.Color
import java.lang.Integer.max
import java.lang.Integer.min

class MessageReactionAddedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReactionAddEvent) onGuildMessageReactionAdd(event)
        if (event is PrivateMessageReactionAddEvent) onPrivateMessageReactionAdd(event)
    }

    private fun onPrivateMessageReactionAdd(event: PrivateMessageReactionAddEvent) = TaskManager.async(event.channel) {
        paginationHandler(event)
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
            val newIndex = min(pagination.messageList.size - 1, max(0, when (emoji) {
                "⏪" -> 0
                "⏩" -> pagination.messageList.size - 1
                "◀️" -> pagination.currentPage - 1
                "▶️" -> pagination.currentPage + 1
                else -> return
            }))

            if (newIndex != pagination.currentPage)
                message.editMessage(pagination.messageList[newIndex]).queue()

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
            val newIndex = min(pagination.messageList.size - 1, max(0, when (emoji) {
                "⏪" -> 0
                "⏩" -> pagination.messageList.size - 1
                "◀️" -> pagination.currentPage - 1
                "▶️" -> pagination.currentPage + 1
                else -> return
            }))

            if (newIndex != pagination.currentPage) {
                val msg = pagination.messageList[newIndex].toMessage()
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

    private fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) = TaskManager.async(event.user, event.channel) {
        selfRoleHandler(event)
        postReactionAddedLog(event)
        verificationHandler(event)
        searchMenuHandler(event)
        paginationHandler(event)
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
            val newIndex = min(pagination.messageList.size - 1, max(0, when (emoji) {
                "⏪" -> 0
                "⏩" -> pagination.messageList.size - 1
                "◀️" -> pagination.currentPage - 1
                "▶️" -> pagination.currentPage + 1
                else -> return
            }))

            if (newIndex != pagination.currentPage)
                message.editMessage(pagination.messageList[newIndex]).queue()

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
            val newIndex = min(pagination.messageList.size - 1, max(0, when (emoji) {
                "⏪" -> 0
                "⏩" -> pagination.messageList.size - 1
                "◀️" -> pagination.currentPage - 1
                "▶️" -> pagination.currentPage + 1
                else -> return
            }))

            if (newIndex != pagination.currentPage) {
                val msg = pagination.messageList[newIndex].toMessage()
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

    private suspend fun searchMenuHandler(event: GuildMessageReactionAddEvent) {
        val guild = event.guild
        if (event.reactionEmote.isEmote && event.user.isBot) return
        val guildPlayer = container.lavaManager.musicPlayerManager.getGuildMusicPlayer(guild)
        val menus = guildPlayer.searchMenus
        val menu = menus.getOrElse(event.messageIdLong, {
            return
        })

        val tracks = menu.audioTracks

        if (event.user.idLong != (tracks.first().userData as TrackUserData).userId) return

        val track: AudioTrack? = when (event.reactionEmote.emoji) {
            "1⃣" -> tracks.getOrElse(0) { return }
            "2⃣" -> tracks.getOrElse(1) { return }
            "3⃣" -> tracks.getOrElse(2) { return }
            "4⃣" -> tracks.getOrElse(3) { return }
            "5⃣" -> tracks.getOrElse(4) { return }
            "❌" -> null
            else -> return
        }

        guildPlayer.searchMenus.remove(event.messageIdLong)
        val message = event.channel.retrieveMessageById(event.messageIdLong).await() ?: return
        val language = getLanguage(container.daoManager, event.user.idLong, guild.idLong)
        when {
            track == null -> {
                val title = i18n.getTranslation(language, "message.music.searchmenu")
                val desc = i18n.getTranslation(language, "message.music.search.cancelled.description")
                val eb = Embedder(container.daoManager, guild.idLong, event.user.idLong, container.settings.botInfo.embedColor)
                    .setTitle(title)
                    .setDescription(desc)
                message.editMessage(eb.build()).queue()
            }
            guildPlayer.safeQueueSilent(container.daoManager, track, menu.nextPosition) -> {
                event.guild.selfMember.voiceState?.channel?.let {
                    LogUtils.addMusicPlayerNewTrack(container.daoManager, container.lavaManager, it, event.user, track)
                }

                val title = i18n.getTranslation(language, "message.music.addedtrack.title")
                    .withVariable(PLACEHOLDER_USER, event.user.asTag)

                val description = i18n.getTranslation(language, "message.music.addedtrack.description")
                    .withVariable("position", guildPlayer.guildTrackManager.getPosition(track).toString())
                    .withVariable("title", track.info.title)
                    .withVariable("duration", getDurationString(track.duration))
                    .withVariable("url", track.info.uri)

                val eb = Embedder(container.daoManager, guild.idLong, event.user.idLong, container.settings.botInfo.embedColor)
                eb.setTitle(title)
                eb.setDescription(description)
                message.editMessage(eb.build()).queue()

            }
            else -> {
                val msg = i18n.getTranslation(language, "message.music.queuelimit")
                    .withVariable("amount", QUEUE_LIMIT.toString())
                    .withVariable("donateAmount", DONATE_QUEUE_LIMIT.toString())
                message.editMessage(msg).queue()
            }
        }
    }

    private suspend fun verificationHandler(event: GuildMessageReactionAddEvent) {
        val textChannel = event.channel
        val guild = event.guild
        val member = event.member
        val dao = container.daoManager

        val verificationChannel = guild.getAndVerifyChannelByType(dao, ChannelType.VERIFICATION, Permission.MESSAGE_MANAGE)
            ?: return
        if (verificationChannel.idLong != textChannel.idLong) return
        val verificationType = dao.verificationTypeWrapper.getType(guild.idLong)
        if (verificationType != VerificationType.REACTION) return

        val unverifiedRole = VerificationUtils.getUnverifiedRoleN(event.channel, dao) ?: return
        if (!dao.unverifiedUsersWrapper.contains(guild.idLong, member.idLong) && !member.roles.contains(unverifiedRole)) {
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
                        VerificationUtils.verify(dao, unverifiedRole, guild.selfMember.user, member)
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
            .withVariable("messageUrl", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.messageId}")
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
        val guild = event.guild
        val member = event.member
        val roles = SelfRoleUtil.getSelectedSelfRoleNByReactionEvent(event, container) ?: return

        for (role in roles) {
            if (!member.roles.contains(role)) {
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
}