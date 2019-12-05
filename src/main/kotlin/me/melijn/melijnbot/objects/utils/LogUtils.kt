package me.melijn.melijnbot.objects.utils

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.music.TrackUserData
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import java.awt.Color

object LogUtils {
    fun sendRemovedChannelLog(language: String, channelType: ChannelType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        sendRemovedChannelLog(language, channelType.toUCC(), logChannel, causePath, causeArg)
    }

    fun sendRemovedMusicChannelLog(language: String, logChannel: TextChannel?, causePath: String, causeArg: String) {
        sendRemovedChannelLog(language, "Music", logChannel, causePath, causeArg)
    }

    fun sendRemovedChannelLog(language: String, type: String, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.channel.title")
            .replace("%type%", type)
        val cause = i18n.getTranslation(language, "logging.removed.channel.causepath.$causePath")
            .replace("%causeArg%", causeArg)


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
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.hitverificationthroughputlimit.title")
        val cause = "```LDIF" + i18n.getTranslation(language, "logging.verification.hitverificationthroughputlimit.description")
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace(PLACEHOLDER_USER_ID, member.id) + "```"

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
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return

        val language = getLanguage(daoManager, -1, guild.idLong)
        val title = i18n.getTranslation(language, "logging.verification.failedaddingrole.title")
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.failedaddingrole.description")
            .replace(PLACEHOLDER_USER_ID, member.id)
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace(PLACEHOLDER_ROLE, role.name)
            .replace(PLACEHOLDER_ROLE_ID, role.id) + "```"

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
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.failed.title")
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.failed.description")
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace(PLACEHOLDER_USER_ID, member.id) + "```"

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
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.verified.title")
            .replace("%author%", author.asTag)
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.verified.description")
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace(PLACEHOLDER_USER_ID, member.id) + "```"

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.GREEN)
        eb.setDescription(description)
        eb.setThumbnail(member.user.effectiveAvatarUrl)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }


    //
    // MUSIC LOG STUFF
    //

    suspend fun sendFailedLoadStreamTrackLog(daoManager: DaoManager, guild: Guild, source: String, exception: FriendlyException) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MUSIC)
            ?: return

        val title = i18n.getTranslation(language, "logging.music.streamurl.loadfailed.title")
        val description = "```LDIF" + i18n.getTranslation(language, "logging.music.streamurl.loadfailed.description")
            .replace("%url%", source)
            .replace("%cause%", exception.message ?: "/") + "```"

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.RED)
        eb.setDescription(description)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun addMusicPlayerResumed(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val track = trackManager.iPlayer.playingTrack ?: return
        val eb = EmbedBuilder()

        val title = context.getTranslation("logging.music.resumed.title")

        val userTitle = context.getTranslation("logging.music.resume.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.resume.userIdfield.title")
        val channel = context.getTranslation("logging.music.resume.channelfield.title")
        val channelId = context.getTranslation("logging.music.resume.channelIdfield.title")
        eb.setTitle(title)


        val vc = context.lavaManager.getConnectedChannel(context.guild)
            ?: throw IllegalArgumentException("Not connected to a channel")
        eb.setDescription("[${track.info.title}](${track.info.uri})")

        eb.addField(userTitle, context.author.asTag, true)
        eb.addField(userIdTitle, context.author.asTag, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setColor(Color.decode("#43b581"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        trackManager.resumeMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }

    suspend fun addMusicPlayerPaused(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val track = trackManager.iPlayer.playingTrack ?: return
        val eb = EmbedBuilder()

        val title = context.getTranslation("logging.music.pause.title")


        val userTitle = context.getTranslation("logging.music.pause.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.pause.userIdfield.title")
        val channel = context.getTranslation("logging.music.pause.channelfield.title")
        val channelId = context.getTranslation("logging.music.pause.channelIdfield.title")
        eb.setTitle(title)

        val vc = context.selfMember.voiceState?.channel ?: throw IllegalArgumentException("NO")
        eb.setDescription("[${track.info.title}](${track.info.uri})")

        eb.addField(userTitle, context.author.asTag, true)
        eb.addField(userIdTitle, context.author.asTag, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setColor(Color.decode("#2f3136"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        trackManager.resumeMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }


    suspend fun sendMusicPlayerException(daoManager: DaoManager, guild: Guild, track: AudioTrack, exception: FriendlyException) {
        val eb = EmbedBuilder()

        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MUSIC, true)
            ?: return
        val title = i18n.getTranslation(language, "logging.music.pause.title")


        val channel = i18n.getTranslation(language, "logging.music.pause.channelfield.title")
        val channelId = i18n.getTranslation(language, "logging.music.pause.channelIdfield.title")
        val cause = i18n.getTranslation(language, "logging.music.pause.causefield.title")
        eb.setTitle(title)

        val vc = Container.instance.lavaManager.getConnectedChannel(guild)
            ?: throw IllegalArgumentException("Not connected to a channel")
        eb.setDescription("[${track.info.title}](${track.info.uri})")
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)
        eb.addField(cause, exception.message ?: "/", false)

        eb.setColor(Color.decode("#2f3136"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun addMusicPlayerNewTrack(context: CommandContext, track: AudioTrack) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val eb = Embedder(context.daoManager, context.guildId, -1, Color.decode("#2f3136").rgb)

        val title = context.getTranslation("logging.music.newtrack.title")


        val userTitle = context.getTranslation("logging.music.newtrack.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.newtrack.userIdfield.title")
        val channel = context.getTranslation("logging.music.newtrack.channelfield.title")
        val channelId = context.getTranslation("logging.music.newtrack.channelIdfield.title")
        eb.setTitle(title)

        val vc = context.lavaManager.getConnectedChannel(context.guild) ?: throw IllegalArgumentException("NO")

        eb.setDescription("[${track.info.title}](${track.info.uri})")
        eb.addField(userTitle, context.author.asTag, true)
        eb.addField(userIdTitle, context.author.id, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setColor(Color.decode("#2f3136"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        trackManager.startMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }

    suspend fun addMusicPlayerNewTrack(daoManager: DaoManager, lavaManager: LavaManager, vc: VoiceChannel, author: User, track: AudioTrack) {
        val guild = vc.guild
        val language = getLanguage(daoManager, -1, guild.idLong)

        val trackManager = lavaManager.musicPlayerManager.getGuildMusicPlayer(guild).guildTrackManager
        val eb = Embedder(daoManager, guild.idLong, -1, Color.decode("#2f3136").rgb)

        val title = i18n.getTranslation(language, "logging.music.newtrack.title")


        val userTitle = i18n.getTranslation(language, "logging.music.newtrack.userfield.title")
        val userIdTitle = i18n.getTranslation(language, "logging.music.newtrack.useridfield.title")
        val channel = i18n.getTranslation(language, "logging.music.newtrack.channelfield.title")
        val channelId = i18n.getTranslation(language, "logging.music.newtrack.channelidfield.title")
        eb.setTitle(title)

        eb.setDescription("[${track.info.title}](${track.info.uri})")
        eb.addField(userTitle, author.asTag, true)
        eb.addField(userIdTitle, author.id, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        trackManager.startMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }

    fun sendRemovedLogChannelLog(language: String, logChannelType: LogChannelType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.logchannel.title")
            .replace("%type%", logChannelType.text)
        val cause = i18n.getTranslation(language, "logging.removed.logchannel.causepath.$causePath")
            .replace("%causeArg%", causeArg)


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#CC0000"))
        eb.setDescription(cause)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        logChannel.sendMessage(eb.build())
    }

    fun sendRemovedRoleLog(language: String, roleType: RoleType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.role.title")
            .replace("%type%", roleType.text)
        val cause = i18n.getTranslation(language, "logging.removed.role.causepath.$causePath")
            .replace("%causeArg%", causeArg)


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#CC0000"))
        eb.setDescription(cause)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        logChannel.sendMessage(eb.build())
    }

    suspend fun sendMessageFailedToRemoveRoleFromMember(daoManager: DaoManager, member: Member, role: Role) {
        val guild = member.guild
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return

        val language = getLanguage(daoManager, -1, guild.idLong)
        val title = i18n.getTranslation(language, "logging.verification.failedremovingrole.title")
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.failedremovingrole.description")
            .replace(PLACEHOLDER_USER_ID, member.id)
            .replace(PLACEHOLDER_USER, member.asTag)
            .replace(PLACEHOLDER_ROLE, role.name)
            .replace(PLACEHOLDER_ROLE_ID, role.id) + "```"

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.RED)
        eb.setDescription(description)
        eb.setThumbnail(member.user.effectiveAvatarUrl)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime())

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }
}