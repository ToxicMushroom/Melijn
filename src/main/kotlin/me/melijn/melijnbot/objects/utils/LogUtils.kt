package me.melijn.melijnbot.objects.utils

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.*
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.music.LavaManager
import me.melijn.melijnbot.objects.music.TrackUserData
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import java.awt.Color
import java.time.ZoneId

object LogUtils {

    fun sendRemovedChannelLog(language: String, zoneId: ZoneId, channelType: ChannelType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        sendRemovedChannelLog(language, zoneId, channelType.toUCC(), logChannel, causePath, causeArg)
    }

    fun sendRemovedMusicChannelLog(language: String, zoneId: ZoneId, logChannel: TextChannel?, causePath: String, causeArg: String) {
        sendRemovedChannelLog(language, zoneId, "Music", logChannel, causePath, causeArg)
    }

    fun sendRemovedChannelLog(language: String, zoneId: ZoneId, type: String, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return

        val title = i18n.getTranslation(language, "logging.removed.channel.title")
            .replace("%type%", type)
        val cause = i18n.getTranslation(language, "logging.removed.channel.causepath.$causePath")
            .replace("%causeArg%", causeArg)


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#7289DA"))
        eb.setDescription(cause)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        logChannel.sendMessage(eb.build())
    }

    suspend fun sendHitVerificationThroughputLimitLog(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        val zoneId = getZoneId(daoManager, member.guild.idLong)
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
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }


    suspend fun sendMessageFailedToAddRoleToMember(daoManager: DaoManager, member: Member, role: Role) {
        val guild = member.guild
        val zoneId = getZoneId(daoManager, guild.idLong)
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
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendFailedVerificationLog(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        val zoneId = getZoneId(daoManager, member.guild.idLong)
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
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendVerifiedUserLog(daoManager: DaoManager, author: User, member: Member) {
        val guild = member.guild
        val zoneId = getZoneId(daoManager, guild.idLong)
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
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }


    //
    // MUSIC LOG STUFF
    //

    suspend fun sendFailedLoadStreamTrackLog(daoManager: DaoManager, guild: Guild, source: String, exception: FriendlyException) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)
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
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun addMusicPlayerResumed(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val zoneId = getZoneId(context.daoManager, context.guild.idLong)
        val track = trackManager.iPlayer.playingTrack ?: return
        val eb = EmbedBuilder()

        val title = context.getTranslation("logging.music.resumed.title")

        val userTitle = context.getTranslation("logging.music.resumed.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.resumed.userIdfield.title")
        val channel = context.getTranslation("logging.music.resumed.channelfield.title")
        val channelId = context.getTranslation("logging.music.resumed.channelIdfield.title")
        eb.setTitle(title)


        val vc = context.lavaManager.getConnectedChannel(context.guild)
            ?: throw IllegalArgumentException("Not connected to a channel")
        eb.setDescription("[${track.info.title}](${track.info.uri})")

        eb.addField(userTitle, context.author.asTag, true)
        eb.addField(userIdTitle, context.author.asTag, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setColor(Color.decode("#43b581"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        trackManager.resumeMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }

    suspend fun addMusicPlayerPaused(context: CommandContext) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val zoneId = getZoneId(context.daoManager, context.guild.idLong)
        val track = trackManager.iPlayer.playingTrack ?: return
        val eb = EmbedBuilder()

        val title = context.getTranslation("logging.music.paused.title")

        val userTitle = context.getTranslation("logging.music.paused.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.paused.userIdfield.title")
        val channel = context.getTranslation("logging.music.paused.channelfield.title")
        val channelId = context.getTranslation("logging.music.paused.channelIdfield.title")
        eb.setTitle(title)

        val vc = context.lavaManager.getConnectedChannel(context.guild)
            ?: throw IllegalArgumentException("Not connected to a channel")
        eb.setDescription("[${track.info.title}](${track.info.uri})")

        eb.addField(userTitle, context.author.asTag, true)
        eb.addField(userIdTitle, context.author.asTag, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setColor(Color.decode("#c4e667"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        trackManager.pauseMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }


    suspend fun sendMusicPlayerException(daoManager: DaoManager, guild: Guild, track: AudioTrack, exception: FriendlyException) {
        val eb = EmbedBuilder()
        val zoneId = getZoneId(daoManager, guild.idLong)
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MUSIC, true)
            ?: return
        val title = i18n.getTranslation(language, "logging.music.exception.title")


        val channel = i18n.getTranslation(language, "logging.music.exception.channelfield.title")
        val channelId = i18n.getTranslation(language, "logging.music.exception.channelIdfield.title")
        val cause = i18n.getTranslation(language, "logging.music.exception.causefield.title")
        eb.setTitle(title)

        val vc = Container.instance.lavaManager.getConnectedChannel(guild)
        eb.setDescription("[${track.info.title}](${track.info.uri})")
        eb.addField(channel, vc?.name ?: "null", true)
        eb.addField(channelId, vc?.id ?: "null", true)
        eb.addField(cause, exception.message ?: "/", false)

        eb.setColor(Color.decode("#cc1010"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun addMusicPlayerNewTrack(context: CommandContext, track: AudioTrack) {
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val eb = Embedder(context.daoManager, context.guildId, -1, Color.decode("#2f3136").rgb)
        val zoneId = getZoneId(context.daoManager, context.guild.idLong)
        val title = context.getTranslation("logging.music.newtrack.title")


        val userTitle = context.getTranslation("logging.music.newtrack.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.newtrack.userIdfield.title")
        val channel = context.getTranslation("logging.music.newtrack.channelfield.title")
        val channelId = context.getTranslation("logging.music.newtrack.channelIdfield.title")
        eb.setTitle(title)

        val vc = context.lavaManager.getConnectedChannel(context.guild)


        eb.setDescription("[${track.info.title}](${track.info.uri})")
        eb.addField(userTitle, context.author.asTag, true)
        eb.addField(userIdTitle, context.author.id, true)
        eb.setColor(Color.decode("#2f3136"))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        if (vc == null) {
            context.taskManager.asyncAfter(2000) {
                val vc2 = context.lavaManager.getConnectedChannel(context.guild)
                eb.addField(channel, vc2?.name ?: "null", true)
                eb.addField(channelId, vc2?.id ?: "null", true)
                trackManager.startMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
            }
        } else {
            eb.addField(channel, vc.name, true)
            eb.addField(channelId, vc.id, true)
            trackManager.startMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
        }
    }

    suspend fun addMusicPlayerNewTrack(daoManager: DaoManager, lavaManager: LavaManager, vc: VoiceChannel, author: User, track: AudioTrack) {
        val guild = vc.guild
        val language = getLanguage(daoManager, -1, guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)

        val trackManager = lavaManager.musicPlayerManager.getGuildMusicPlayer(guild).guildTrackManager
        val eb = Embedder(daoManager, guild.idLong, -1, Color.decode("#2f3136").rgb)

        val title = i18n.getTranslation(language, "logging.music.newtrack.title")


        val userTitle = i18n.getTranslation(language, "logging.music.newtrack.userfield.title")
        val userIdTitle = i18n.getTranslation(language, "logging.music.newtrack.userIdfield.title")
        val channel = i18n.getTranslation(language, "logging.music.newtrack.channelfield.title")
        val channelId = i18n.getTranslation(language, "logging.music.newtrack.channelIdfield.title")
        eb.setTitle(title)

        eb.setDescription("[${track.info.title}](${track.info.uri})")
        eb.addField(userTitle, author.asTag, true)
        eb.addField(userIdTitle, author.id, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        trackManager.startMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }

    fun sendRemovedLogChannelLog(language: String, zoneId: ZoneId, logChannelType: LogChannelType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.logchannel.title")
            .replace("%type%", logChannelType.text)
        val cause = i18n.getTranslation(language, "logging.removed.logchannel.causepath.$causePath")
            .replace("%causeArg%", causeArg)


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#CC0000"))
        eb.setDescription(cause)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        logChannel.sendMessage(eb.build())
    }

    fun sendRemovedRoleLog(language: String, zoneId: ZoneId, roleType: RoleType, logChannel: TextChannel?, causePath: String, causeArg: String) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.role.title")
            .replace("%type%", roleType.toUCC())
        val cause = i18n.getTranslation(language, "logging.removed.role.causepath.$causePath")
            .replace("%causeArg%", causeArg)


        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#CC0000"))
        eb.setDescription(cause)
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        logChannel.sendMessage(eb.build())
    }

    suspend fun sendMessageFailedToRemoveRoleFromMember(daoManager: DaoManager, member: Member, role: Role) {
        val guild = member.guild
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return
        val zoneId = getZoneId(daoManager, member.guild.idLong)

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
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendRoleAddedLog(container: Container, adder: User, target: User, role: Role) {
//        val daoManager = container.daoManager
//        val guild = role.guild
//        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.ROLES)
//            ?: return
//
//        val language = getLanguage(daoManager, -1, guild.idLong)
//        val title = i18n.getTranslation(language, "logging.role.member.added.title")
//        val description = "```LDIF" + i18n.getTranslation(language, "logging.role.member.added.description")
//            .replace(PLACEHOLDER_USER_ID, adder.id)
//            .replace(PLACEHOLDER_USER, adder.asTag)
//            .replace("%targetId%", target.id)
//            .replace("%target%", target.asTag)
//            .replace(PLACEHOLDER_ROLE, role.name)
//            .replace(PLACEHOLDER_ROLE_ID, role.id) + "```"
//        if (adder.idLong == adder.jda.selfUser.idLong) {
//            container.roleAddedMap
//        }
//
//        val eb = EmbedBuilder()
//        eb.setTitle(title)
//        eb.setColor(role.color)
//        eb.setDescription(description)
//        eb.setThumbnail(target.effectiveAvatarUrl)
//        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(), adder.effectiveAvatarUrl)
//
//        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendRoleRemovedLog(container: Container, remover: User, target: User, role: Role) {
//        val daoManager = container.daoManager
//        val guild = role.guild
//        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.ROLES)
//            ?: return
//
//        val language = getLanguage(daoManager, -1, guild.idLong)
//        val title = i18n.getTranslation(language, "logging.role.member.removed.title")
//        val description = "```LDIF" + i18n.getTranslation(language, "logging.role.member.removed.description")
//            .replace(PLACEHOLDER_USER_ID, remover.id)
//            .replace(PLACEHOLDER_USER, remover.asTag)
//            .replace("%targetId%", target.id)
//            .replace("%target%", target.asTag)
//            .replace(PLACEHOLDER_ROLE, role.name)
//            .replace(PLACEHOLDER_ROLE_ID, role.id) + "```"
//
//        val eb = EmbedBuilder()
//        eb.setTitle(title)
//        eb.setColor(role.color)
//        eb.setDescription(description)
//        eb.setThumbnail(target.effectiveAvatarUrl)
//        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(), remover.effectiveAvatarUrl)
//
//        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendPPGainedMessageDMAndLC(container: Container, message: Message, pointsTriggerType: PointsTriggerType, causeArgs: Map<String, List<String>>, pp: Int) {
        val guild = message.guild
        val zoneId = getZoneId(container.daoManager, guild.idLong)
        val daoManager = container.daoManager
        val lc = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.PUNISHMENT_POINTS) ?: return
        val language = getLanguage(daoManager, -1, guild.idLong)

        val title = i18n.getTranslation(language, "logging.punishmentpoints.title")
        val lcBodyPart = i18n.getTranslation(language, "logging.punishmentpoints.description.extra.lc")
            .replace("%target%", message.author.asTag)
            .replace("%targetId%", message.author.id)

        var lcBody = i18n.getTranslation(language, "logging.punishmentpoints.description")
            .replace("%channel%", message.textChannel.asTag)
            .replace("%channelId%", message.textChannel.id)
            .replace("%message%", message.contentRaw)
            .replace("%messageId%", message.id)
            .replace("%points%", "$pp")
            .replace("%moment%", System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        var extra = ""
        for ((key, value) in causeArgs) {
            if (value.isEmpty()) continue
            extra += i18n.getTranslation(language, "logging.punishmentpoints.cause.${key}")
                .replace("%word%", value.joinToString()) + "\n"
        }

        lcBody = lcBody.replace("%extra%", extra)

        val eb = EmbedBuilder()
        eb.setTitle(title)
        eb.setColor(Color.decode("#c596ff"))

        val dmBody = i18n.getTranslation(language, "logging.punishmentpoints.description.extra.dm")
            .replace("%guild%", message.guild.name)
            .replace("%guildId%", message.guild.id) + lcBody
        eb.setDescription("```LDIF\n$dmBody```")

        val pc = message.author.openPrivateChannel().awaitOrNull()
        if (pc != null) {
            val success = pc.sendMessage(eb.build()).awaitBool()
            if (!success) {
                val dm = i18n.getTranslation(language, "logging.punishmentpoints.description.extra.dmfailed")
                lcBody += dm
            }
        } else {
            val pcm = i18n.getTranslation(language, "logging.punishmentpoints.description.extra.openingpcfailed")
            lcBody += pcm
        }

        eb.setDescription("```LDIF\n$lcBodyPart$lcBody```")
        sendEmbed(daoManager.embedDisabledWrapper, lc, eb.build())
    }

    suspend fun sendBirthdayMessage(daoManager: DaoManager, textChannel: TextChannel, member: Member, birthYear: Int?) {
        val guildId = textChannel.guild.idLong
        val messageType = MessageType.BIRTHDAY
        val language = getLanguage(daoManager, guildId)
        val messageWrapper = daoManager.messageWrapper
        var message = messageWrapper.messageCache.get(Pair(guildId, messageType)).await()
        if (message == null) {
            val msg = i18n.getTranslation(language, "logging.birthday")
                .replace("%user%", member.asTag)

            sendMsg(textChannel, msg)
        } else {
            if (MessageCommandUtil.removeMessageIfEmpty(guildId, messageType, message, messageWrapper)) return

            message = BirthdayUtil.replaceVariablesInBirthdayMessage(daoManager, member, message, birthYear)

            val msg: Message? = message.toMessage()
            when {
                msg == null -> sendAttachments(textChannel, message.attachments)
                message.attachments.isNotEmpty() -> sendMsgWithAttachments(textChannel, msg, message.attachments)
                else -> sendMsg(textChannel, msg, failed = { t -> t.printStackTrace() })
            }
        }
    }
}