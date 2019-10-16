package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import java.awt.Color

object LogUtils {
    fun sendRemovedChannelLog(language: String, channel: ChannelType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.channel.title")
            .replace("%type%", channel.toUCC())
        val cause = "```LDIF" + i18n.getTranslation(language, "logging.removed.channel.causePath.$causePath")
            .replace("%causeArg%", causeArg) + "```"


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#7289DA"))
        eb.setDescription(cause)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        logChannel.sendMessage(eb.build())
    }

    suspend fun sendHitVerificationThroughputLimitLog(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(LogChannelType.VERIFICATION, daoManager.logChannelWrapper)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.hitverificationthroughputlimit.title")
        val cause = "```LDIF" + i18n.getTranslation(language, "logging.verification.hitverificationthroughputlimit.description")
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace("%userId%", member.id) + "```"

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.ORANGE)
        eb.setDescription(cause)
        eb.setThumbnail(member.user.effectiveAvatarUrl)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }


    suspend fun sendMessageFailedToAddRoleToMember(daoManager: DaoManager, member: Member, role: Role) {
        val guild = member.guild
        val logChannel = guild.getAndVerifyLogChannelByType(LogChannelType.VERIFICATION, daoManager.logChannelWrapper)
            ?: return

        val language = getLanguage(daoManager, -1, guild.idLong)
        val title = i18n.getTranslation(language, "logging.verification.failedaddingrole.title")
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.failedaddingrole.description")
            .replace("%userId%", member.id)
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace(PLACEHOLDER_ROLE, role.name)
            .replace("%roleId%", role.id) + "```"

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.RED)
        eb.setDescription(description)
        eb.setThumbnail(member.user.effectiveAvatarUrl)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendFailedVerificationLog(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(LogChannelType.VERIFICATION, daoManager.logChannelWrapper)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.failed.title")
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.failed.description")
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace("%userId%", member.id) + "```"

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.RED)
        eb.setDescription(description)
        eb.setThumbnail(member.user.effectiveAvatarUrl)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendVerifiedUserLog(daoManager: DaoManager, author: User, member: Member) {
        val guild = member.guild
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(LogChannelType.VERIFICATION, daoManager.logChannelWrapper)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.verified.title")
            .replace("%author%", author.asTag)
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.verified.description")
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace("%userId%", member.id) + "```"

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.GREEN)
        eb.setDescription(description)
        eb.setThumbnail(member.user.effectiveAvatarUrl)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }
}