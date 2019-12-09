package me.melijn.melijnbot.objects.events.eventlisteners

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.SelfRoleUtil
import me.melijn.melijnbot.objects.music.DONATE_QUEUE_LIMIT
import me.melijn.melijnbot.objects.music.QUEUE_LIMIT
import me.melijn.melijnbot.objects.music.TrackUserData
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import java.awt.Color

class MessageReactionAddedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReactionAddEvent) onGuildMessageReactionAdd(event)
    }

    private fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) = runBlocking {
        selfRoleHandler(event)
        postReactionAddedLog(event)
        verificationHandler(event)
        searchMenuHandler(event)
    }

    private suspend fun searchMenuHandler(event: GuildMessageReactionAddEvent) {
        val guild = event.guild
        if (event.reactionEmote.isEmote) return
        val guildPlayer = container.lavaManager.musicPlayerManager.getGuildMusicPlayer(guild)
        val menus = guildPlayer.searchMenus
        val menu = menus.getOrElse(event.messageIdLong, { return })
        if (event.user.idLong != (menu.first().userData as TrackUserData).userId) return

        val track: AudioTrack? = when (event.reactionEmote.emoji) {
            "1⃣" -> menu.getOrElse(0) { return }
            "2⃣" -> menu.getOrElse(1) { return }
            "3⃣" -> menu.getOrElse(2) { return }
            "4⃣" -> menu.getOrElse(3) { return }
            "5⃣" -> menu.getOrElse(4) { return }
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
                val eb = Embedder(container.daoManager, guild.idLong, event.user.idLong, container.settings.embedColor)
                eb.setTitle(title)
                eb.setDescription(desc)
                message.editMessage(eb.build()).queue()
            }
            guildPlayer.safeQueueSilent(container.daoManager, track) -> {
                event.guild.selfMember.voiceState?.channel?.let {
                    LogUtils.addMusicPlayerNewTrack(container.daoManager, container.lavaManager, it, event.user, track)
                }

                val title = i18n.getTranslation(language, "message.music.addedtrack.title")
                    .replace(PLACEHOLDER_USER, event.user.asTag)

                val description = i18n.getTranslation(language, "message.music.addedtrack.description")
                    .replace("%position%", guildPlayer.guildTrackManager.getPosition(track).toString())
                    .replace("%title%", track.info.title)
                    .replace("%duration%", getDurationString(track.duration))
                    .replace("%url%", track.info.uri)

                val eb = Embedder(container.daoManager, guild.idLong, event.user.idLong, container.settings.embedColor)
                eb.setTitle(title)
                eb.setDescription(description)
                message.editMessage(eb.build()).queue()

            }
            else -> {
                val msg = i18n.getTranslation(language, "message.music.queuelimit")
                    .replace("%amount%", QUEUE_LIMIT.toString())
                    .replace("%donateAmount%", DONATE_QUEUE_LIMIT.toString())
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

        val unverifiedRole = VerificationUtils.getUnverifiedRoleN(event.channel, dao) ?: return
        if (!dao.unverifiedUsersWrapper.contains(guild.idLong, member.idLong) && !member.roles.contains(unverifiedRole)) {
            //User is already verified
            if (!member.hasPermission(Permission.ADMINISTRATOR)) {
                //User doesn't have admin perms to add reaction in verification channel
                event.reaction.removeReaction(event.user).queue()
            }
            return
        }

        val verificationType = dao.verificationTypeWrapper.verificationTypeCache[guild.idLong].await()
        verificationType?.let {
            when (it) {
                VerificationType.REACTION -> {
                    val code = dao.verificationEmotejiWrapper.verificationEmotejiCache[guild.idLong].await()
                    if (
                        (event.reactionEmote.isEmoji && event.reactionEmote.emoji == code) ||
                        (event.reactionEmote.isEmote && event.reactionEmote.emote.id == code)) {
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

        val embedBuilder = EmbedBuilder()
        val language = getLanguage(dao, -1, event.guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.reaction.log.title")
            .replace(PLACEHOLDER_CHANNEL, event.channel.asTag)

        val isEmote = event.reactionEmote.isEmote
        val part = if (isEmote) "emote" else "emoji"
        val description = i18n.getTranslation(language, "listener.message.reaction.$part.log.description")
            .replace(PLACEHOLDER_USER_ID, event.member.id)
            .replace("%messageId%", event.messageId)
            .replace("%emoteName%", event.reactionEmote.name)
            .replace("%emoteId%", if (isEmote) event.reactionEmote.id else "/")
            .replace("%moment%", System.currentTimeMillis().asEpochMillisToDateTime())
            .replace("%messageUrl%", "https://discordapp.com/channels/${event.guild.id}/${event.channel.id}/${event.messageId}")
            .replace("%emoteUrl%", if (isEmote) event.reactionEmote.emote.imageUrl else "/")

        embedBuilder.setTitle(title)
        embedBuilder.setDescription(description)
        embedBuilder.setThumbnail(if (isEmote) event.reactionEmote.emote.imageUrl else null)
        val footer = i18n.getTranslation(language, "listener.message.reaction.log.footer")
            .replace(PLACEHOLDER_USER, event.member.asTag)
        embedBuilder.setFooter(footer, event.member.user.effectiveAvatarUrl)
        embedBuilder.setColor(Color.WHITE)

        sendEmbed(dao.embedDisabledWrapper, logChannel, embedBuilder.build())
    }

    private suspend fun selfRoleHandler(event: GuildMessageReactionAddEvent) {
        val guild = event.guild
        val member = event.member
        val role = SelfRoleUtil.getSelectedSelfRoleNByReactionEvent(event, container) ?: return

        if (!member.roles.contains(role)) {
            val added = guild.addRoleToMember(member, role).awaitBool()
            if (!added) {
                LogUtils.sendMessageFailedToAddRoleToMember(container.daoManager, member, role)
            }
        }
    }
}