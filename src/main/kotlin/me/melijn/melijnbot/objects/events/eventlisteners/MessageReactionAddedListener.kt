package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.SelfRoleUtil
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.VerificationUtils
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.asTag
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.objects.utils.sendEmbed
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
    }

    private suspend fun verificationHandler(event: GuildMessageReactionAddEvent) {
        val textChannel = event.channel
        val guild = event.guild
        val member = event.member
        val dao = container.daoManager

        val verificationChannel = guild.getAndVerifyChannelByType(ChannelType.VERIFICATION, dao, Permission.MESSAGE_MANAGE)
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
                        (event.reactionEmote.isEmote && event.reactionEmote.emote.id == code))
                    {
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
        val logChannel = event.guild.getAndVerifyLogChannelByType(LogChannelType.REACTION, dao.logChannelWrapper)
            ?: return

        val embedBuilder = EmbedBuilder()
        val language = getLanguage(dao, -1, event.guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.reaction.log.title")
            .replace("%channel%", event.channel.asTag)

        val isEmote = event.reactionEmote.isEmote
        val part = if (isEmote) "emote" else "emoji"
        val description = i18n.getTranslation(language, "listener.message.reaction.$part.log.description")
            .replace("%userId%", event.member.id)
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
            guild.addRoleToMember(member, role).queue()
        }
    }
}