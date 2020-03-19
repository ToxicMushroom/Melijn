package me.melijn.melijnbot.database

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.database.audio.*
import me.melijn.melijnbot.database.autopunishment.*
import me.melijn.melijnbot.database.ban.BanDao
import me.melijn.melijnbot.database.ban.BanWrapper
import me.melijn.melijnbot.database.ban.SoftBanDao
import me.melijn.melijnbot.database.ban.SoftBanWrapper
import me.melijn.melijnbot.database.birthday.BirthdayDao
import me.melijn.melijnbot.database.birthday.BirthdayHistoryDao
import me.melijn.melijnbot.database.birthday.BirthdayHistoryWrapper
import me.melijn.melijnbot.database.birthday.BirthdayWrapper
import me.melijn.melijnbot.database.channel.*
import me.melijn.melijnbot.database.command.*
import me.melijn.melijnbot.database.cooldown.CommandChannelCooldownDao
import me.melijn.melijnbot.database.cooldown.CommandChannelCooldownWrapper
import me.melijn.melijnbot.database.cooldown.CommandCooldownDao
import me.melijn.melijnbot.database.cooldown.CommandCooldownWrapper
import me.melijn.melijnbot.database.disabled.ChannelCommandStateDao
import me.melijn.melijnbot.database.disabled.ChannelCommandStateWrapper
import me.melijn.melijnbot.database.disabled.DisabledCommandDao
import me.melijn.melijnbot.database.disabled.DisabledCommandWrapper
import me.melijn.melijnbot.database.embed.*
import me.melijn.melijnbot.database.filter.FilterDao
import me.melijn.melijnbot.database.filter.FilterGroupDao
import me.melijn.melijnbot.database.filter.FilterGroupWrapper
import me.melijn.melijnbot.database.filter.FilterWrapper
import me.melijn.melijnbot.database.kick.KickDao
import me.melijn.melijnbot.database.kick.KickWrapper
import me.melijn.melijnbot.database.language.GuildLanguageDao
import me.melijn.melijnbot.database.language.GuildLanguageWrapper
import me.melijn.melijnbot.database.language.UserLanguageDao
import me.melijn.melijnbot.database.language.UserLanguageWrapper
import me.melijn.melijnbot.database.logchannel.LogChannelDao
import me.melijn.melijnbot.database.logchannel.LogChannelWrapper
import me.melijn.melijnbot.database.message.MessageDao
import me.melijn.melijnbot.database.message.MessageHistoryDao
import me.melijn.melijnbot.database.message.MessageHistoryWrapper
import me.melijn.melijnbot.database.message.MessageWrapper
import me.melijn.melijnbot.database.mute.MuteDao
import me.melijn.melijnbot.database.mute.MuteWrapper
import me.melijn.melijnbot.database.permission.*
import me.melijn.melijnbot.database.prefix.GuildPrefixDao
import me.melijn.melijnbot.database.prefix.GuildPrefixWrapper
import me.melijn.melijnbot.database.prefix.UserPrefixDao
import me.melijn.melijnbot.database.prefix.UserPrefixWrapper
import me.melijn.melijnbot.database.role.*
import me.melijn.melijnbot.database.supporter.SupporterWrapper
import me.melijn.melijnbot.database.supporter.UserSupporterDao
import me.melijn.melijnbot.database.time.TimeZoneDao
import me.melijn.melijnbot.database.time.TimeZoneWrapper
import me.melijn.melijnbot.database.verification.*
import me.melijn.melijnbot.database.votes.VoteDao
import me.melijn.melijnbot.database.votes.VoteWrapper
import me.melijn.melijnbot.database.warn.WarnDao
import me.melijn.melijnbot.database.warn.WarnWrapper
import me.melijn.melijnbot.objects.threading.TaskManager

const val RAPIDLY_USED_CACHE = 1L
const val NOT_IMPORTANT_CACHE = 2L
const val FREQUENTLY_USED_CACHE = 5L
const val IMPORTANT_CACHE = 10L

const val HUGE_CACHE = 500L
const val LARGE_CACHE = 200L
const val NORMAL_CACHE = 100L
const val SMALL_CACHE = 50L

class DaoManager(taskManager: TaskManager, dbSettings: Settings.Database) {

    companion object {
        val afterTableFunctions = mutableListOf<() -> Unit>()
    }

    val tracksWrapper: TracksWrapper
    val songCacheWrapper: SongCacheWrapper
    val gainProfileWrapper: GainProfileWrapper
    val music247Wrapper: Music247Wrapper
    val musicNodeWrapper: MusicNodeWrapper
    val streamUrlWrapper: StreamUrlWrapper
    val musicChannelWrapper: MusicChannelWrapper
    val commandWrapper: CommandWrapper
    val commandUsageWrapper: CommandUsageWrapper
    val customCommandWrapper: CustomCommandWrapper

    val guildLanguageWrapper: GuildLanguageWrapper
    val userLanguageWrapper: UserLanguageWrapper

    val rolePermissionWrapper: RolePermissionWrapper
    val userPermissionWrapper: UserPermissionWrapper
    val channelRolePermissionWrapper: ChannelRolePermissionWrapper
    val channelUserPermissionWrapper: ChannelUserPermissionWrapper

    val disabledCommandWrapper: DisabledCommandWrapper
    val channelCommandStateWrapper: ChannelCommandStateWrapper

    val commandChannelCoolDownWrapper: CommandChannelCooldownWrapper
    val commandCooldownWrapper: CommandCooldownWrapper

    val guildPrefixWrapper: GuildPrefixWrapper
    val userPrefixWrapper: UserPrefixWrapper

    val supporterWrapper: SupporterWrapper

    val embedDisabledWrapper: EmbedDisabledWrapper
    val embedColorWrapper: EmbedColorWrapper
    val userEmbedColorWrapper: UserEmbedColorWrapper

    val logChannelWrapper: LogChannelWrapper
    val channelWrapper: ChannelWrapper

    val roleWrapper: RoleWrapper
    val tempRoleWrapper: TempRoleWrapper
    val selfRoleWrapper: SelfRoleWrapper

    lateinit var dbVersion: String
    lateinit var connectorVersion: String

    val banWrapper: BanWrapper
    val muteWrapper: MuteWrapper
    val kickWrapper: KickWrapper
    val warnWrapper: WarnWrapper
    val softBanWrapper: SoftBanWrapper

    val messageHistoryWrapper: MessageHistoryWrapper
    val messageWrapper: MessageWrapper
    val forceRoleWrapper: ForceRoleWrapper

    val verificationCodeWrapper: VerificationCodeWrapper
    val verificationEmotejiWrapper: VerificationEmotejiWrapper
    val verificationTypeWrapper: VerificationTypeWrapper
    val verificationUserFlowRateWrapper: VerificationUserFlowRateWrapper
    val unverifiedUsersWrapper: UnverifiedUsersWrapper

    val filterWrapper: FilterWrapper //All filters
    val filterGroupWrapper: FilterGroupWrapper //Groups of filters with info like state, channels and name
    val autoPunishmentWrapper: AutoPunishmentWrapper //keeps track of users
    val autoPunishmentGroupWrapper: PunishmentGroupWrapper //keeps track of punishment ladders/groups (points -> punishment)
    val punishmentWrapper: PunishmentWrapper //preconfigured punishments

    val birthdayWrapper: BirthdayWrapper
    val birthdayHistoryWrapper: BirthdayHistoryWrapper
    val timeZoneWrapper: TimeZoneWrapper

    val voteWrapper: VoteWrapper
    var driverManager: DriverManager

    init {
        driverManager = DriverManager(dbSettings
            //, dbSettings.mySQL
        )

        runBlocking {
            dbVersion = driverManager.getDBVersion()
            connectorVersion = driverManager.getConnectorVersion()
        }

        tracksWrapper = TracksWrapper(TracksDao(driverManager), LastVoiceChannelDao(driverManager))
        songCacheWrapper = SongCacheWrapper(SongCacheDao(driverManager))
        gainProfileWrapper = GainProfileWrapper(taskManager, GainProfileDao(driverManager))
        musicNodeWrapper = MusicNodeWrapper(taskManager, MusicNodeDao(driverManager))
        music247Wrapper = Music247Wrapper(taskManager, Music247Dao(driverManager))

        streamUrlWrapper = StreamUrlWrapper(taskManager, StreamUrlDao(driverManager))

        commandWrapper = CommandWrapper(taskManager, CommandDao(driverManager))
        commandUsageWrapper = CommandUsageWrapper(taskManager, CommandUsageDao(driverManager))
        customCommandWrapper = CustomCommandWrapper(taskManager, CustomCommandDao((driverManager)))

        guildLanguageWrapper = GuildLanguageWrapper(taskManager, GuildLanguageDao(driverManager))
        userLanguageWrapper = UserLanguageWrapper(taskManager, UserLanguageDao(driverManager))

        rolePermissionWrapper = RolePermissionWrapper(taskManager, RolePermissionDao(driverManager))
        userPermissionWrapper = UserPermissionWrapper(taskManager, UserPermissionDao(driverManager))
        channelRolePermissionWrapper = ChannelRolePermissionWrapper(taskManager, ChannelRolePermissionDao(driverManager))
        channelUserPermissionWrapper = ChannelUserPermissionWrapper(taskManager, ChannelUserPermissionDao(driverManager))

        disabledCommandWrapper = DisabledCommandWrapper(taskManager, DisabledCommandDao(driverManager))

        channelCommandStateWrapper = ChannelCommandStateWrapper(taskManager, ChannelCommandStateDao(driverManager))

        commandCooldownWrapper = CommandCooldownWrapper(taskManager, CommandCooldownDao(driverManager))
        commandChannelCoolDownWrapper = CommandChannelCooldownWrapper(taskManager, CommandChannelCooldownDao(driverManager))

        guildPrefixWrapper = GuildPrefixWrapper(taskManager, GuildPrefixDao(driverManager))
        userPrefixWrapper = UserPrefixWrapper(taskManager, UserPrefixDao(driverManager))

        supporterWrapper = SupporterWrapper(taskManager, UserSupporterDao(driverManager))

        embedDisabledWrapper = EmbedDisabledWrapper(taskManager, EmbedDisabledDao(driverManager))
        embedColorWrapper = EmbedColorWrapper(taskManager, EmbedColorDao(driverManager))
        userEmbedColorWrapper = UserEmbedColorWrapper(taskManager, UserEmbedColorDao(driverManager))

        logChannelWrapper = LogChannelWrapper(taskManager, LogChannelDao(driverManager))
        channelWrapper = ChannelWrapper(taskManager, ChannelDao(driverManager))
        musicChannelWrapper = MusicChannelWrapper(taskManager, MusicChannelDao(driverManager))
        roleWrapper = RoleWrapper(taskManager, RoleDao(driverManager))
        selfRoleWrapper = SelfRoleWrapper(taskManager, SelfRoleDao(driverManager))
        tempRoleWrapper = TempRoleWrapper(taskManager, TempRoleDao(driverManager))

        banWrapper = BanWrapper(taskManager, BanDao(driverManager))
        muteWrapper = MuteWrapper(taskManager, MuteDao(driverManager))
        kickWrapper = KickWrapper(taskManager, KickDao(driverManager))
        warnWrapper = WarnWrapper(taskManager, WarnDao(driverManager))
        softBanWrapper = SoftBanWrapper(taskManager, SoftBanDao(driverManager))

        messageHistoryWrapper = MessageHistoryWrapper(taskManager, MessageHistoryDao(driverManager))
        messageWrapper = MessageWrapper(taskManager, MessageDao(driverManager))
        forceRoleWrapper = ForceRoleWrapper(taskManager, ForceRoleDao(driverManager))

        verificationCodeWrapper = VerificationCodeWrapper(taskManager, VerificationCodeDao(driverManager))
        verificationEmotejiWrapper = VerificationEmotejiWrapper(taskManager, VerificationEmotejiDao(driverManager))
        verificationTypeWrapper = VerificationTypeWrapper(taskManager, VerificationTypeDao(driverManager))
        verificationUserFlowRateWrapper = VerificationUserFlowRateWrapper(taskManager, VerificationUserFlowRateDao(driverManager))
        unverifiedUsersWrapper = UnverifiedUsersWrapper(taskManager, UnverifiedUsersDao(driverManager))

        filterWrapper = FilterWrapper(taskManager, FilterDao(driverManager))
        filterGroupWrapper = FilterGroupWrapper(taskManager, FilterGroupDao(driverManager))
        autoPunishmentWrapper = AutoPunishmentWrapper(taskManager, AutoPunishmentDao(driverManager))
        autoPunishmentGroupWrapper = PunishmentGroupWrapper(taskManager, PunishmentGroupDao(driverManager))
        punishmentWrapper = PunishmentWrapper(taskManager, PunishmentDao(driverManager))

        birthdayWrapper = BirthdayWrapper(BirthdayDao(driverManager))
        birthdayHistoryWrapper = BirthdayHistoryWrapper(BirthdayHistoryDao(driverManager))
        timeZoneWrapper = TimeZoneWrapper(taskManager, TimeZoneDao(driverManager))

        voteWrapper = VoteWrapper(VoteDao(driverManager))
        //After registering wrappers
        driverManager.executeTableRegistration()
        for (func in afterTableFunctions) {
            func()
        }
    }
}