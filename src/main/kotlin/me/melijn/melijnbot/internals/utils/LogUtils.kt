package me.melijn.melijnbot.internals.utils

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import io.ktor.client.*
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.settings.VoteReminderOption
import me.melijn.melijnbot.enums.*
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.music.LavaManager
import me.melijn.melijnbot.internals.music.TrackUserData
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.message.*
import me.melijn.melijnbot.internals.web.rest.voted.BotList
import me.melijn.melijnbot.internals.web.rest.voted.getBotListTimeOut
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.awt.Color
import java.time.Instant
import java.time.ZoneId

object LogUtils {

    fun sendRemovedChannelLog(
        language: String,
        zoneId: ZoneId,
        channelType: ChannelType,
        logChannel: TextChannel?,
        causePath: String,
        causeArg: String
    ) {
        sendRemovedChannelLog(language, zoneId, channelType.toUCC(), logChannel, causePath, causeArg)
    }

    fun sendRemovedMusicChannelLog(
        language: String,
        zoneId: ZoneId,
        logChannel: TextChannel?,
        causePath: String,
        causeArg: String
    ) {
        sendRemovedChannelLog(language, zoneId, "Music", logChannel, causePath, causeArg)
    }

    private fun sendRemovedChannelLog(
        language: String,
        zoneId: ZoneId,
        type: String,
        logChannel: TextChannel?,
        causePath: String,
        causeArg: String
    ) {
        if (logChannel == null) return

        val title = i18n.getTranslation(language, "logging.removed.channel.title")
            .withVariable("type", type)
        val cause = i18n.getTranslation(language, "logging.removed.channel.causepath.$causePath")
            .withVariable("causeArg", causeArg)


        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color(0x7289DA))
            .setDescription(cause)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        logChannel.sendMessage(eb.build())
    }

    suspend fun sendHitVerificationThroughputLimitLog(daoManager: DaoManager, member: Member) {
        val guild = member.guild
        val zoneId = getZoneId(daoManager, member.guild.idLong)
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.hitverificationthroughputlimit.title")
        val cause =
            "```LDIF" + i18n.getTranslation(language, "logging.verification.hitverificationthroughputlimit.description")
                .withSafeVariable(PLACEHOLDER_USER, member.asTag)
                .withVariable(PLACEHOLDER_USER_ID, member.id) + "```"

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color.ORANGE)
            .setDescription(cause)
            .setThumbnail(member.user.effectiveAvatarUrl)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

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
            .withVariable(PLACEHOLDER_USER_ID, member.id)
            .withSafeVariable(PLACEHOLDER_USER, member.asTag)
            .withSafeVariable(PLACEHOLDER_ROLE, role.name)
            .withVariable(PLACEHOLDER_ROLE_ID, role.id) + "```"

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color.RED)
            .setDescription(description)
            .setThumbnail(member.user.effectiveAvatarUrl)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

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
            .withSafeVariable(PLACEHOLDER_USER, member.asTag)
            .withVariable(PLACEHOLDER_USER_ID, member.id) + "```"

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color.RED)
            .setDescription(description)
            .setThumbnail(member.user.effectiveAvatarUrl)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun sendVerifiedUserLog(daoManager: DaoManager, author: User, member: Member) {
        val guild = member.guild
        val zoneId = getZoneId(daoManager, guild.idLong)
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return

        val title = i18n.getTranslation(language, "logging.verification.verified.title")
            .withSafeVariable("author", author.asTag)
        val description = "```LDIF" + i18n.getTranslation(language, "logging.verification.verified.description")
            .withSafeVariable(PLACEHOLDER_USER, member.asTag)
            .withVariable(PLACEHOLDER_USER_ID, member.id) + "```"

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color.GREEN)
            .setDescription(description)
            .setThumbnail(member.user.effectiveAvatarUrl)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }


    //
    // MUSIC LOG STUFF
    //

    suspend fun sendFailedLoadStreamTrackLog(
        daoManager: DaoManager,
        guild: Guild,
        source: String,
        exception: FriendlyException
    ) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.MUSIC)
            ?: return

        val title = i18n.getTranslation(language, "logging.music.streamurl.loadfailed.title")
        val description = "```LDIF" + i18n.getTranslation(language, "logging.music.streamurl.loadfailed.description")
            .withVariable("url", source)
            .withSafeVariable("cause", exception.message ?: "/") + "```"

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color.RED)
            .setDescription(description)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun addMusicPlayerResumed(context: ICommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val zoneId = getZoneId(context.daoManager, context.guild.idLong)
        val track = trackManager.iPlayer.playingTrack ?: return

        val vc = context.lavaManager.getConnectedChannel(context.guild)
            ?: throw IllegalArgumentException("Not connected to a channel")


        val title = context.getTranslation("logging.music.resumed.title")

        val userTitle = context.getTranslation("logging.music.resumed.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.resumed.userIdfield.title")
        val channel = context.getTranslation("logging.music.resumed.channelfield.title")
        val channelId = context.getTranslation("logging.music.resumed.channelIdfield.title")

        val eb = EmbedBuilder()
            .setTitle(title)
            .setDescription("[%title%](${track.info.uri})".withSafeVariable("title", track.info.uri))
            .addField(userTitle, context.author.asTag, true)
            .addField(userIdTitle, context.author.asTag, true)
            .addField(channel, vc.name, true)
            .addField(channelId, vc.id, true)
            .setColor(Color(0x43b581))
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        trackManager.resumeMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }

    suspend fun addMusicPlayerPaused(context: ICommandContext) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val zoneId = getZoneId(context.daoManager, context.guild.idLong)
        val track = trackManager.iPlayer.playingTrack ?: return


        val title = context.getTranslation("logging.music.paused.title")

        val userTitle = context.getTranslation("logging.music.paused.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.paused.userIdfield.title")
        val channel = context.getTranslation("logging.music.paused.channelfield.title")
        val channelId = context.getTranslation("logging.music.paused.channelIdfield.title")

        val vc = context.lavaManager.getConnectedChannel(context.guild)
            ?: throw IllegalArgumentException("Not connected to a channel")

        val eb = EmbedBuilder()
            .setTitle(title)
            .setDescription("[%title%](${track.info.uri})".withSafeVariable("title", track.info.title))
            .addField(userTitle, context.author.asTag, true)
            .addField(userIdTitle, context.author.asTag, true)
            .addField(channel, vc.name, true)
            .addField(channelId, vc.id, true)
            .setColor(Color(0xc4e667))
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        trackManager.pauseMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }


    suspend fun sendMusicPlayerException(
        daoManager: DaoManager,
        guild: Guild,
        track: AudioTrack,
        exception: FriendlyException
    ) {
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
        eb.setDescription("[%title%](${track.info.uri})".withSafeVariable("title", track.info.title))
        eb.addField(channel, vc?.name ?: "null", true)
        eb.addField(channelId, vc?.id ?: "null", true)
        eb.addField(cause, exception.message ?: "/", false)

        eb.setColor(Color(0xcc1010))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        sendEmbed(daoManager.embedDisabledWrapper, logChannel, eb.build())
    }

    suspend fun addMusicPlayerNewTrack(context: ICommandContext, track: AudioTrack) {
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val eb = Embedder(context.daoManager, context.guildId, -1)
            .setColor(Color(0x2f3136))
        val zoneId = getZoneId(context.daoManager, context.guild.idLong)
        val title = context.getTranslation("logging.music.newtrack.title")


        val userTitle = context.getTranslation("logging.music.newtrack.userfield.title")
        val userIdTitle = context.getTranslation("logging.music.newtrack.userIdfield.title")
        val channel = context.getTranslation("logging.music.newtrack.channelfield.title")
        val channelId = context.getTranslation("logging.music.newtrack.channelIdfield.title")
        eb.setTitle(title)

        val vc = context.lavaManager.getConnectedChannel(context.guild)


        eb.setDescription("[%title%](${track.info.uri})".withSafeVariable("title", track.info.title))
        eb.addField(userTitle, context.author.asTag, true)
        eb.addField(userIdTitle, context.author.id, true)
        eb.setColor(Color(0x2f3136))
        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        if (vc == null) {
            TaskManager.asyncAfter(2000) {
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

    suspend fun addMusicPlayerNewTrack(
        daoManager: DaoManager,
        lavaManager: LavaManager,
        vc: VoiceChannel,
        author: User,
        track: AudioTrack
    ) {
        val guild = vc.guild
        val language = getLanguage(daoManager, -1, guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)

        val trackManager = lavaManager.musicPlayerManager.getGuildMusicPlayer(guild).guildTrackManager
        val eb = Embedder(daoManager, guild.idLong, -1)
            .setColor(Color(0x2f3136))

        val title = i18n.getTranslation(language, "logging.music.newtrack.title")


        val userTitle = i18n.getTranslation(language, "logging.music.newtrack.userfield.title")
        val userIdTitle = i18n.getTranslation(language, "logging.music.newtrack.userIdfield.title")
        val channel = i18n.getTranslation(language, "logging.music.newtrack.channelfield.title")
        val channelId = i18n.getTranslation(language, "logging.music.newtrack.channelIdfield.title")
        eb.setTitle(title)

        eb.setDescription("[%title%](${track.info.uri})".withSafeVariable("title", track.info.title))
        eb.addField(userTitle, author.asTag, true)
        eb.addField(userIdTitle, author.id, true)
        eb.addField(channel, vc.name, true)
        eb.addField(channelId, vc.id, true)

        eb.setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        trackManager.startMomentMessageMap[(track.userData as TrackUserData).currentTime] = eb.build()
    }

    fun sendRemovedLogChannelLog(
        language: String,
        zoneId: ZoneId,
        logChannelType: LogChannelType,
        logChannel: TextChannel?,
        causePath: String,
        causeArg: String
    ) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.logchannel.title")
            .withVariable("type", logChannelType.text)
        val cause = i18n.getTranslation(language, "logging.removed.logchannel.causepath.$causePath")
            .withVariable("causeArg", causeArg)


        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color(0xCC0000))
            .setDescription(cause)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        logChannel.sendMessage(eb.build())
    }

    fun sendRemovedRoleLog(
        language: String,
        zoneId: ZoneId,
        roleType: RoleType,
        logChannel: TextChannel?,
        causePath: String,
        causeArg: String
    ) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.role.title")
            .withVariable("type", roleType.toUCC())
        val cause = i18n.getTranslation(language, "logging.removed.role.causepath.$causePath")
            .withVariable("causeArg", causeArg)


        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color(0xCC0000))
            .setDescription(cause)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        logChannel.sendMessage(eb.build())
    }

    suspend fun sendMessageFailedToRemoveRoleFromMember(daoManager: DaoManager, member: Member, role: Role) {
        val guild = member.guild
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.VERIFICATION)
            ?: return
        val zoneId = getZoneId(daoManager, member.guild.idLong)

        val language = getLanguage(daoManager, -1, guild.idLong)
        val title = i18n.getTranslation(language, "logging.verification.failedremovingrole.title")
        val description =
            "```LDIF" + i18n.getTranslation(language, "logging.verification.failedremovingrole.description")
                .withVariable(PLACEHOLDER_USER_ID, member.id)
                .withSafeVariable(PLACEHOLDER_USER, member.asTag)
                .withSafeVariable(PLACEHOLDER_ROLE, role.name)
                .withVariable(PLACEHOLDER_ROLE_ID, role.id) + "```"

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color.RED)
            .setDescription(description)
            .setThumbnail(member.user.effectiveAvatarUrl)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

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
//            .withVariable(PLACEHOLDER_USER_ID, adder.id)
//            .withVariable(PLACEHOLDER_USER, adder.asTag)
//            .withVariable("targetId", target.id)
//            .withVariable("target", target.asTag)
//            .withVariable(PLACEHOLDER_ROLE, role.name)
//            .withVariable(PLACEHOLDER_ROLE_ID, role.id) + "```"
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
//            .withVariable(PLACEHOLDER_USER_ID, remover.id)
//            .withVariable(PLACEHOLDER_USER, remover.asTag)
//            .withVariable("targetId", target.id)
//            .withVariable("target", target.asTag)
//            .withVariable(PLACEHOLDER_ROLE, role.name)
//            .withVariable(PLACEHOLDER_ROLE_ID, role.id) + "```"
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

    suspend fun sendPPGainedMessageDMAndLC(
        container: Container,
        message: Message,
        pointsTriggerType: PointsTriggerType,
        causeArgs: Map<String, List<String>>,
        pp: Int
    ) {
        val guild = message.guild
        val zoneId = getZoneId(container.daoManager, guild.idLong)
        val daoManager = container.daoManager
        val lc = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.PUNISHMENT_POINTS) ?: return
        val language = getLanguage(daoManager, -1, guild.idLong)

        val title = i18n.getTranslation(language, "logging.punishmentpoints.title")
        val lcBodyPart = i18n.getTranslation(language, "logging.punishmentpoints.description.extra.lc")
            .withSafeVariable("target", message.author.asTag)
            .withVariable("targetId", message.author.id)

        var lcBody = i18n.getTranslation(language, "logging.punishmentpoints.description")
            .withSafeVariable("channel", message.textChannel.asTag)
            .withVariable("channelId", message.textChannel.id)
            .withSafeVariable("message", message.contentRaw)
            .withVariable("messageId", message.id)
            .withVariable("points", "$pp")
            .withVariable("moment", System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        var extra = ""
        for ((key, value) in causeArgs) {
            if (value.isEmpty()) continue
            extra += i18n.getTranslation(language, "logging.punishmentpoints.cause.${key}")
                .withSafeVariable("word", value.joinToString()) + "\n"
        }

        lcBody = lcBody.withVariable("extra", extra)

        val dmBody = i18n.getTranslation(language, "logging.punishmentpoints.description.extra.dm")
            .withSafeVariable("server", message.guild.name)
            .withVariable("serverId", message.guild.id) + lcBody

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color(0xc596ff))
            .setDescription("```LDIF\n$dmBody```")

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

    suspend fun sendBirthdayMessage(
        daoManager: DaoManager,
        httpClient: HttpClient,
        textChannel: TextChannel,
        member: Member,
        birthYear: Int?
    ) {
        val guildId = textChannel.guild.idLong
        val messageType = MessageType.BIRTHDAY
        val language = getLanguage(daoManager, guildId)
        val messageWrapper = daoManager.messageWrapper
        var message = messageWrapper.getMessage(guildId, messageType)
        if (message == null) {
            val msg = i18n.getTranslation(language, "logging.birthday")
                .withSafeVariable("user", member.asTag)

            sendRspOrMsg(textChannel, daoManager, msg)
        } else {
            if (MessageCommandUtil.removeMessageIfEmpty(guildId, messageType, message, messageWrapper)) return

            message = BirthdayUtil.replaceVariablesInBirthdayMessage(daoManager, member, message, birthYear)

            val msg: Message? = message.toMessage()
            when {
                msg == null -> sendAttachments(textChannel, httpClient, message.attachments)
                message.attachments.isNotEmpty() -> sendMsgWithAttachments(
                    textChannel,
                    httpClient,
                    msg,
                    message.attachments
                )
                else -> sendMsg(textChannel, msg, failed = { t -> t.printStackTrace() })
            }
        }
    }

    fun sendRemovedSelfRoleLog(
        language: String,
        zoneId: ZoneId,
        emoteji: String,
        logChannel: TextChannel?,
        causePath: String,
        causeArg: String
    ) {
        if (logChannel == null) return
        val title = i18n.getTranslation(language, "logging.removed.selfrole.title")
            .withVariable("emoteji", emoteji)

        val cause = i18n.getTranslation(language, "logging.removed.selfrole.causepath.$causePath")
            .withVariable("causeArg", causeArg)

        val eb = EmbedBuilder()
            .setTitle(title)
            .setColor(Color(0xCC0000))
            .setDescription(cause)
            .setFooter(System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        logChannel.sendMessage(eb.build())
    }

    suspend fun sendPurgeLog(context: ICommandContext, messages: List<Message>) {
        val guild = context.guild
        val daoManager = context.daoManager
        val pmLogChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.PURGED_MESSAGE)
            ?: return
        val botLogState = daoManager.botLogStateWrapper.shouldLog(pmLogChannel.guild.idLong)
        val zoneId = getZoneId(daoManager, guild.idLong)

        val channel: TextChannel = messages.first().textChannel

        val sb = StringBuilder()
        var groupDay = 0

        for (msg in messages.sortedBy { it.idLong }) {
            val day = msg.timeCreated.dayOfMonth
            val author = msg.author
            if (!botLogState && author.isBot) continue

            if (day != groupDay) {
                groupDay = day
                sb
                    .append("\n\n*")
                    .append(msg.timeCreated.asEpochMillisToDate(zoneId))
                    .append("*")
            }

            sb.append("\n`")
                .append(msg.timeCreated.asEpochMillisToTimeInvis(zoneId))
                .append("` **")
                .append(author.name)

            if (msg.author.idLong != context.authorId) {
                sb.append(" • ")
                    .append(author.id)
            }

            sb.append(":** ")
                .append(MarkdownSanitizer.escape(escapeForLog(msg.contentRaw)))


        }


        val language = getLanguage(daoManager, -1, guild.idLong)
        val title = i18n.getTranslation(language, "listener.message.purge.log.title")
            .withSafeVariable(PLACEHOLDER_CHANNEL, channel.asTag)
            .withVariable("amount", "${messages.size}")

        val description = i18n.getTranslation(language, "listener.message.purge.log.description")
            .withVariable("content", sb.toString())
            .withVariable("messageDeleterId", "${context.authorId}")
            .withVariable("deletedTime", System.currentTimeMillis().asEpochMillisToDateTime(zoneId))

        val ebs = mutableListOf<EmbedBuilder>()
        val embedBuilder = EmbedBuilder()
            .setTitle(title)
            .setColor(Color(0x927ca6))

        if (description.length > MessageEmbed.TEXT_MAX_LENGTH) {
            val parts = StringUtils.splitMessage(description, maxLength = MessageEmbed.TEXT_MAX_LENGTH)
            embedBuilder.setDescription(parts[0])
            ebs.add(embedBuilder)
            for (part in parts.subList(1, parts.size)) {
                val embedBuilder2 = EmbedBuilder()
                embedBuilder2.setColor(Color(0x927ca6))
                embedBuilder2.setDescription(part)
                ebs.add(embedBuilder2)
            }
        } else {
            embedBuilder.setDescription(description)
            ebs.add(embedBuilder)
        }

        for ((index, eb) in ebs.withIndex()) {

            if (index == ebs.size - 1) {
                val footer = i18n.getTranslation(language, "listener.message.purge.log.footer")
                    .withSafeVariable(PLACEHOLDER_USER, context.author.asTag)
                eb.setFooter(footer, context.author.effectiveAvatarUrl)
            }

            sendEmbed(daoManager.embedDisabledWrapper, pmLogChannel, eb.build())
        }
    }

    suspend fun sendReceivedVoteRewards(
        container: Container,
        userId: Long,
        newBalance: Long,
        baseMel: Long,
        totalMel: Long,
        streak: Long,
        votes: Long,
        site: String
    ) {
        val user = MelijnBot.shardManager.retrieveUserById(userId).await()
        val pc = user.openPrivateChannel().awaitOrNull() ?: return

        val extraMel = if (totalMel - baseMel > 0) {
            "100 + ${totalMel - baseMel}"
        } else "100"
        val embedder = Embedder(container.daoManager, -1, userId)
            .setTitle("Vote Received from $site")
            .setDescription(
                "Thanks for voting, you received **$extraMel** mel. Your new balance is **$newBalance** mel\n" +
                    "Note: bonus mel is calculated using: premium status, speed, weekend, total votes and streak."
            )
            .addField("Current Streak", "$streak", true)
            .addField("Total Votes", votes.toString(), true)
            .build()

        sendEmbed(pc, embedder)
    }


    const val VOTE_LINKS = "TopGG:  %statusOne% - `12h` - use `uBlock Origin` to block/skip ads"

    suspend fun sendVoteReminder(daoManager: DaoManager, flag: Int, userId: Long) {
        val user = MelijnBot.shardManager.retrieveUserById(userId).await()
        val pc = user.openPrivateChannel().awaitOrNull() ?: return

        val userVote = daoManager.voteWrapper.getUserVote(userId) ?: return
        val cMillis = System.currentTimeMillis()

        val botlist = getBotListFromFlag(VoteReminderOption.values().first { it.number == flag })
        val embedder = Embedder(daoManager, -1, userId)
            .setTitle("Your vote for $botlist is ready (o゜▽゜)o☆")
            .setDescription("This is a reminder that you can vote again")
            .addField(
                "top.gg",
                getVoteStatusForSite(VoteReminderOption.TOPGG, userVote.topggLastTime - cMillis),
                true
            )
//            .addField(
//                "discordbotlist.com",
//                getVoteStatusForSite(VoteReminderOption.DBLCOM, offset),
//                true
//            )
//            .addField(
//                "botsfordiscord.com",
//                getVoteStatusForSite(VoteReminderOption.BFDCOM, offset),
//                true
//            )
//            .addField(
//                "discord.boats",
//                getVoteStatusForSite(VoteReminderOption.DBOATS, offset),
//                true
//            )
            .addField("Current Streak", userVote.streak.toString(), true)
            .setFooter("You can disable this reminder with >toggleVoteReminder")
            .build()

        try { // Handle closed dms
            sendEmbed(pc, embedder)
        } catch (t: Throwable) {
        }
    }

    fun getVoteStatusForSite(opt: VoteReminderOption, offset: Long): String {
        val readyOne = getBotListTimeOut(BotList.TOP_GG) + offset
        val voteLink = getBotListVoteLinkFromFlag(opt)
        return "**[${
            if (readyOne <= 1000L) {
                "vote ready"
            } else {
                getDurationString(readyOne)
            }
        }]($voteLink)**"
    }

    private fun getBotListVoteLinkFromFlag(opt: VoteReminderOption): String {
        return when (opt) {
            VoteReminderOption.TOPGG -> "https://top.gg/bot/melijn/vote"
            VoteReminderOption.DBLCOM -> "https://discordbotlist.com/bots/melijn/upvote"
            VoteReminderOption.BFDCOM -> "https://botsfordiscord.com/bot/368362411591204865/vote"
            VoteReminderOption.DBOATS -> "https://discord.boats/bot/368362411591204865/vote"
            VoteReminderOption.GLOBAL -> "all sites"
        }
    }


    private fun getBotListFromFlag(opt: VoteReminderOption): String {
        return when (opt) {
            VoteReminderOption.TOPGG -> "top.gg"
            VoteReminderOption.DBLCOM -> "discordbotlist.com"
            VoteReminderOption.BFDCOM -> "botsfordiscord.com"
            VoteReminderOption.DBOATS -> "discord.boats"
            VoteReminderOption.GLOBAL -> "all sites"
        }
    }

    suspend fun sendReceivedVoteTest(container: Container, userId: Long, botlist: String) {
        val user = MelijnBot.shardManager.retrieveUserById(userId).await()
        val pc = user.openPrivateChannel().awaitOrNull() ?: return
        val embedder = Embedder(container.daoManager, -1, userId)
            .setTitle("TestVote Received from $botlist")
            .build()

        sendEmbed(pc, embedder)
    }

    suspend fun sendReminder(daoManager: DaoManager, userId: Long, remindAt: Long, message: String) {
        val user = MelijnBot.shardManager.retrieveUserById(userId).await()
        val pc = user.openPrivateChannel().awaitOrNull() ?: return
        val embedder = Embedder(daoManager, -1, userId)
            .setTitle("Your Reminder")
            .setDescription("message: $message")
            .setTimestamp(Instant.ofEpochMilli(remindAt))
            .build()

        sendEmbed(pc, embedder)
    }
}