package me.melijn.melijnbot.database

import me.melijn.melijnbot.database.alias.AliasDao
import me.melijn.melijnbot.database.alias.AliasWrapper
import me.melijn.melijnbot.database.autopunishment.*
import me.melijn.melijnbot.database.ban.*
import me.melijn.melijnbot.database.birthday.BirthdayDao
import me.melijn.melijnbot.database.birthday.BirthdayHistoryDao
import me.melijn.melijnbot.database.birthday.BirthdayHistoryWrapper
import me.melijn.melijnbot.database.birthday.BirthdayWrapper
import me.melijn.melijnbot.database.channel.*
import me.melijn.melijnbot.database.command.*
import me.melijn.melijnbot.database.cooldown.*
import me.melijn.melijnbot.database.disabled.ChannelCommandStateDao
import me.melijn.melijnbot.database.disabled.ChannelCommandStateWrapper
import me.melijn.melijnbot.database.disabled.DisabledCommandDao
import me.melijn.melijnbot.database.disabled.DisabledCommandWrapper
import me.melijn.melijnbot.database.economy.BalanceDao
import me.melijn.melijnbot.database.economy.BalanceWrapper
import me.melijn.melijnbot.database.economy.EconomyCooldownDao
import me.melijn.melijnbot.database.economy.EconomyCooldownWrapper
import me.melijn.melijnbot.database.embed.*
import me.melijn.melijnbot.database.filter.FilterDao
import me.melijn.melijnbot.database.filter.FilterGroupDao
import me.melijn.melijnbot.database.filter.FilterGroupWrapper
import me.melijn.melijnbot.database.filter.FilterWrapper
import me.melijn.melijnbot.database.games.*
import me.melijn.melijnbot.database.join.AutoRemoveInactiveJoinMessageDao
import me.melijn.melijnbot.database.join.AutoRemoveInactiveJoinMessageWrapper
import me.melijn.melijnbot.database.join.InactiveJMDao
import me.melijn.melijnbot.database.join.InactiveJMWrapper
import me.melijn.melijnbot.database.kick.KickDao
import me.melijn.melijnbot.database.kick.KickWrapper
import me.melijn.melijnbot.database.language.GuildLanguageDao
import me.melijn.melijnbot.database.language.GuildLanguageWrapper
import me.melijn.melijnbot.database.language.UserLanguageDao
import me.melijn.melijnbot.database.language.UserLanguageWrapper
import me.melijn.melijnbot.database.locking.LockExcludedDao
import me.melijn.melijnbot.database.locking.LockExcludedWrapper
import me.melijn.melijnbot.database.logchannel.LogChannelDao
import me.melijn.melijnbot.database.logchannel.LogChannelWrapper
import me.melijn.melijnbot.database.message.*
import me.melijn.melijnbot.database.mute.MuteDao
import me.melijn.melijnbot.database.mute.MuteWrapper
import me.melijn.melijnbot.database.newyear.NewYearDao
import me.melijn.melijnbot.database.newyear.NewYearWrapper
import me.melijn.melijnbot.database.permission.*
import me.melijn.melijnbot.database.prefix.GuildPrefixDao
import me.melijn.melijnbot.database.prefix.GuildPrefixWrapper
import me.melijn.melijnbot.database.prefix.UserPrefixDao
import me.melijn.melijnbot.database.prefix.UserPrefixWrapper
import me.melijn.melijnbot.database.ratelimit.RatelimitWrapper
import me.melijn.melijnbot.database.reminder.ReminderDao
import me.melijn.melijnbot.database.reminder.ReminderWrapper
import me.melijn.melijnbot.database.rep.RepDao
import me.melijn.melijnbot.database.rep.RepWrapper
import me.melijn.melijnbot.database.role.*
import me.melijn.melijnbot.database.scripts.ScriptCooldownDao
import me.melijn.melijnbot.database.scripts.ScriptCooldownWrapper
import me.melijn.melijnbot.database.scripts.ScriptDao
import me.melijn.melijnbot.database.scripts.ScriptWrapper
import me.melijn.melijnbot.database.settings.*
import me.melijn.melijnbot.database.starboard.StarboardMessageDao
import me.melijn.melijnbot.database.starboard.StarboardMessageWrapper
import me.melijn.melijnbot.database.starboard.StarboardSettingsDao
import me.melijn.melijnbot.database.starboard.StarboardSettingsWrapper
import me.melijn.melijnbot.database.statesync.EmoteCacheDao
import me.melijn.melijnbot.database.supporter.SupporterDao
import me.melijn.melijnbot.database.supporter.SupporterWrapper
import me.melijn.melijnbot.database.time.TimeZoneDao
import me.melijn.melijnbot.database.time.TimeZoneWrapper
import me.melijn.melijnbot.database.validemojis.ValidEmojiDao
import me.melijn.melijnbot.database.validemojis.ValidEmojiWrapper
import me.melijn.melijnbot.database.verification.*
import me.melijn.melijnbot.database.votes.VoteDao
import me.melijn.melijnbot.database.votes.VoteReminderDao
import me.melijn.melijnbot.database.votes.VoteReminderWrapper
import me.melijn.melijnbot.database.votes.VoteWrapper
import me.melijn.melijnbot.database.warn.WarnDao
import me.melijn.melijnbot.database.warn.WarnWrapper
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.threading.TaskManager

const val RAPIDLY_USED_CACHE = 1L
const val NOT_IMPORTANT_CACHE = 2L
const val FREQUENTLY_USED_CACHE = 5L
const val IMPORTANT_CACHE = 10L
const val HIGHER_CACHE = 10
const val NORMAL_CACHE = 5
const val SHORT_CACHE = 3
const val RAPID_CACHE = 1

const val HUGE_CACHE = 500L
const val LARGE_CACHE = 200L
const val NORMAL_CACHE_SIZE = 100L
const val SMALL_CACHE = 50L

class DaoManager(dbSettings: Settings.Database, redisSettings: Settings.Redis, secretKey: String) {

    companion object {
        val afterTableFunctions = mutableListOf<() -> Unit>()
    }

    var driverManager: DriverManager = DriverManager(dbSettings, redisSettings)

    val bannedUsers: BannedUserCacheDao
    val emoteCache: EmoteCacheDao
    val streamUrlWrapper: StreamUrlWrapper
    val musicChannelWrapper: MusicChannelWrapper
    val commandWrapper: CommandWrapper
    val commandUsageWrapper: CommandUsageWrapper
    val customCommandWrapper: CustomCommandWrapper
    val scriptWrapper: ScriptWrapper
    val scriptCooldownWrapper: ScriptCooldownWrapper

    val guildLanguageWrapper: GuildLanguageWrapper
    val userLanguageWrapper: UserLanguageWrapper

    val rolePermissionWrapper: RolePermissionWrapper
    val userPermissionWrapper: UserPermissionWrapper
    val channelRolePermissionWrapper: ChannelRolePermissionWrapper
    val channelUserPermissionWrapper: ChannelUserPermissionWrapper
    val discordChannelOverridesWrapper: DiscordChannelOverridesWrapper
    val lockExcludedWrapper: LockExcludedWrapper

    val disabledCommandWrapper: DisabledCommandWrapper
    val channelCommandStateWrapper: ChannelCommandStateWrapper

    val commandChannelCoolDownWrapper: CommandChannelCooldownWrapper
    val commandCooldownWrapper: CommandCooldownWrapper

    val guildPrefixWrapper: GuildPrefixWrapper
    val userPrefixWrapper: UserPrefixWrapper
    val allowSpacedPrefixWrapper: AllowSpacedPrefixWrapper
    val aliasWrapper: AliasWrapper

    val supporterWrapper: SupporterWrapper

    val embedDisabledWrapper: EmbedDisabledWrapper
    val embedColorWrapper: EmbedColorWrapper
    val userEmbedColorWrapper: UserEmbedColorWrapper

    val logChannelWrapper: LogChannelWrapper
    val channelWrapper: ChannelWrapper

    val roleWrapper: RoleWrapper
    val tempRoleWrapper: TempRoleWrapper
    val joinRoleWrapper: JoinRoleWrapper
    val joinRoleGroupWrapper: JoinRoleGroupWrapper
    val selfRoleWrapper: SelfRoleWrapper
    val selfRoleGroupWrapper: SelfRoleGroupWrapper
    val selfRoleModeWrapper: SelfRoleModeWrapper

    lateinit var dbVersion: String
    lateinit var connectorVersion: String

    val banWrapper: BanWrapper
    val muteWrapper: MuteWrapper
    val kickWrapper: KickWrapper
    val warnWrapper: WarnWrapper
    val softBanWrapper: SoftBanWrapper
    val botBannedWrapper: BotBannedWrapper

    val messageHistoryWrapper: MessageHistoryWrapper
    val linkedMessageWrapper: LinkedMessageWrapper
    val messageWrapper: MessageWrapper
    val forceRoleWrapper: ForceRoleWrapper
    val channelRoleWrapper: ChannelRoleWrapper
    val userChannelRoleWrapper: UserChannelRoleWrapper

    val verificationPasswordWrapper: VerificationPasswordWrapper
    val verificationEmotejiWrapper: VerificationEmotejiWrapper
    val verificationTypeWrapper: VerificationTypeWrapper
    val verificationUserFlowRateWrapper: VerificationUserFlowRateWrapper
    val unverifiedUsersWrapper: UnverifiedUsersWrapper

    val filterWrapper: FilterWrapper //All filters
    val filterGroupWrapper: FilterGroupWrapper //Groups of filters with info like state, channels and name
    val spamWrapper: SpamWrapper // Settings for spam
    val spamGroupWrapper: SpamGroupWrapper //Groups of spams with info like state, channels and name

    val autoPunishmentWrapper: AutoPunishmentWrapper //keeps track of users
    val autoPunishmentGroupWrapper: PunishmentGroupWrapper //keeps track of punishment ladders/groups (points -> punishment)
    val punishmentWrapper: PunishmentWrapper //preconfigured punishments

    val birthdayWrapper: BirthdayWrapper
    val birthdayHistoryWrapper: BirthdayHistoryWrapper
    val timeZoneWrapper: TimeZoneWrapper

    val bannedOrKickedTriggersLeaveWrapper: BannedOrKickedTriggersLeaveWrapper
    val botLogStateWrapper: BotLogStateWrapper
    val removeResponseWrapper: RemoveResponseWrapper
    val removeInvokeWrapper: RemoveInvokeWrapper
    val voteReminderStatesWrapper: VoteReminderStatesWrapper
    val voteReminderWrapper: VoteReminderWrapper
    val reminderWrapper: ReminderWrapper

    val rpsWrapper: RPSWrapper
    val tttWrapper: TTTWrapper

    val voteWrapper: VoteWrapper
    val balanceWrapper: BalanceWrapper
    val repWrapper: RepWrapper
    val economyCooldownWrapper: EconomyCooldownWrapper
    val globalCooldownWrapper: GlobalCooldownWrapper

    val starboardSettingsWrapper: StarboardSettingsWrapper
    val starboardMessageWrapper: StarboardMessageWrapper


    val osuWrapper: OsuWrapper

    val newYearWrapper: NewYearWrapper

    val autoRemoveInactiveJoinMessageWrapper: AutoRemoveInactiveJoinMessageWrapper
    val inactiveJMWrapper: InactiveJMWrapper

    val rateLimitWrapper: RatelimitWrapper

    val validEmojiWrapper: ValidEmojiWrapper


    init {
        TaskManager.async {
            dbVersion = driverManager.getDBVersion()
            connectorVersion = driverManager.getConnectorVersion()
        }

        bannedUsers = BannedUserCacheDao(driverManager)

        emoteCache = EmoteCacheDao(driverManager)
        streamUrlWrapper = StreamUrlWrapper(StreamUrlDao(driverManager))

        commandWrapper = CommandWrapper(CommandDao(driverManager))
        commandUsageWrapper = CommandUsageWrapper(CommandUsageDao(driverManager))
        customCommandWrapper = CustomCommandWrapper(CustomCommandDao(driverManager))
        scriptWrapper = ScriptWrapper(ScriptDao(driverManager))
        scriptCooldownWrapper = ScriptCooldownWrapper(ScriptCooldownDao(driverManager))

        guildLanguageWrapper = GuildLanguageWrapper(GuildLanguageDao(driverManager))
        userLanguageWrapper = UserLanguageWrapper(UserLanguageDao(driverManager))

        rolePermissionWrapper = RolePermissionWrapper(RolePermissionDao(driverManager))
        userPermissionWrapper = UserPermissionWrapper(UserPermissionDao(driverManager))
        channelRolePermissionWrapper = ChannelRolePermissionWrapper(ChannelRolePermissionDao(driverManager))
        channelUserPermissionWrapper = ChannelUserPermissionWrapper(ChannelUserPermissionDao(driverManager))
        discordChannelOverridesWrapper = DiscordChannelOverridesWrapper(DiscordChannelOverridesDao(driverManager))
        lockExcludedWrapper = LockExcludedWrapper(LockExcludedDao(driverManager))

        disabledCommandWrapper = DisabledCommandWrapper(DisabledCommandDao(driverManager))

        channelCommandStateWrapper = ChannelCommandStateWrapper(ChannelCommandStateDao(driverManager))

        commandCooldownWrapper = CommandCooldownWrapper(CommandCooldownDao(driverManager))
        commandChannelCoolDownWrapper = CommandChannelCooldownWrapper(CommandChannelCooldownDao(driverManager))

        guildPrefixWrapper = GuildPrefixWrapper(GuildPrefixDao(driverManager))
        userPrefixWrapper = UserPrefixWrapper(UserPrefixDao(driverManager))
        allowSpacedPrefixWrapper = AllowSpacedPrefixWrapper(
            AllowSpacedPrefixDao(driverManager), PrivateAllowSpacedPrefixDao(driverManager)
        )
        aliasWrapper = AliasWrapper(AliasDao(driverManager))

        supporterWrapper = SupporterWrapper(SupporterDao(driverManager))

        embedDisabledWrapper = EmbedDisabledWrapper(EmbedDisabledDao(driverManager))
        embedColorWrapper = EmbedColorWrapper(EmbedColorDao(driverManager))
        userEmbedColorWrapper = UserEmbedColorWrapper(UserEmbedColorDao(driverManager))

        logChannelWrapper = LogChannelWrapper(LogChannelDao(driverManager))
        channelWrapper = ChannelWrapper(ChannelDao(driverManager))
        musicChannelWrapper = MusicChannelWrapper(MusicChannelDao(driverManager))
        roleWrapper = RoleWrapper(RoleDao(driverManager))
        joinRoleWrapper = JoinRoleWrapper(JoinRoleDao(driverManager))
        joinRoleGroupWrapper = JoinRoleGroupWrapper(JoinRoleGroupDao(driverManager))
        selfRoleWrapper = SelfRoleWrapper(SelfRoleDao(driverManager))
        selfRoleGroupWrapper = SelfRoleGroupWrapper(SelfRoleGroupDao(driverManager))
        selfRoleModeWrapper = SelfRoleModeWrapper(SelfRoleModeDao(driverManager))
        tempRoleWrapper = TempRoleWrapper(TempRoleDao(driverManager))

        banWrapper = BanWrapper(BanDao(driverManager))
        muteWrapper = MuteWrapper(MuteDao(driverManager))
        kickWrapper = KickWrapper(KickDao(driverManager))
        warnWrapper = WarnWrapper(WarnDao(driverManager))
        softBanWrapper = SoftBanWrapper(SoftBanDao(driverManager))
        botBannedWrapper = BotBannedWrapper(BotBannedDao(driverManager))

        messageHistoryWrapper = MessageHistoryWrapper(MessageHistoryDao(driverManager, secretKey))
        linkedMessageWrapper = LinkedMessageWrapper(LinkedMessageDao(driverManager))
        messageWrapper = MessageWrapper(MessageDao(driverManager))
        forceRoleWrapper = ForceRoleWrapper(ForceRoleDao(driverManager))
        channelRoleWrapper = ChannelRoleWrapper(ChannelRoleDao(driverManager))
        userChannelRoleWrapper = UserChannelRoleWrapper(UserChannelRoleDao(driverManager))

        verificationPasswordWrapper = VerificationPasswordWrapper(VerificationPasswordDao(driverManager))
        verificationEmotejiWrapper = VerificationEmotejiWrapper(VerificationEmotejiDao(driverManager))
        verificationTypeWrapper = VerificationTypeWrapper(VerificationTypeDao(driverManager))
        verificationUserFlowRateWrapper = VerificationUserFlowRateWrapper(VerificationUserFlowRateDao(driverManager))
        unverifiedUsersWrapper = UnverifiedUsersWrapper(UnverifiedUsersDao(driverManager))

        filterWrapper = FilterWrapper(FilterDao(driverManager))
        filterGroupWrapper = FilterGroupWrapper(FilterGroupDao(driverManager))
        spamWrapper = SpamWrapper(SpamDao(driverManager))
        spamGroupWrapper = SpamGroupWrapper(SpamGroupDao(driverManager))
        autoPunishmentWrapper = AutoPunishmentWrapper(AutoPunishmentDao(driverManager))
        autoPunishmentGroupWrapper = PunishmentGroupWrapper(PunishmentGroupDao(driverManager))
        punishmentWrapper = PunishmentWrapper(PunishmentDao(driverManager))

        birthdayWrapper = BirthdayWrapper(BirthdayDao(driverManager))
        birthdayHistoryWrapper = BirthdayHistoryWrapper(BirthdayHistoryDao(driverManager))
        timeZoneWrapper = TimeZoneWrapper(TimeZoneDao(driverManager))

        bannedOrKickedTriggersLeaveWrapper = BannedOrKickedTriggersLeaveWrapper(BannedOrKickedTriggersLeaveDao(driverManager))
        botLogStateWrapper = BotLogStateWrapper(BotLogStateDao(driverManager))
        removeResponseWrapper = RemoveResponseWrapper(RemoveResponsesDao(driverManager))
        removeInvokeWrapper = RemoveInvokeWrapper(RemoveInvokeDao(driverManager))
        voteReminderStatesWrapper = VoteReminderStatesWrapper(VoteReminderStatesDao(driverManager))
        voteReminderWrapper = VoteReminderWrapper(VoteReminderDao(driverManager))
        reminderWrapper = ReminderWrapper(ReminderDao(driverManager))

        rpsWrapper = RPSWrapper(RPSDao(driverManager))
        tttWrapper = TTTWrapper(TTTDao(driverManager))

        voteWrapper = VoteWrapper(VoteDao(driverManager))
        balanceWrapper = BalanceWrapper(BalanceDao(driverManager))
        repWrapper = RepWrapper(RepDao(driverManager))
        economyCooldownWrapper = EconomyCooldownWrapper(EconomyCooldownDao(driverManager))
        globalCooldownWrapper = GlobalCooldownWrapper(GlobalCooldownDao(driverManager))

        starboardSettingsWrapper = StarboardSettingsWrapper(StarboardSettingsDao(driverManager))
        starboardMessageWrapper = StarboardMessageWrapper(StarboardMessageDao(driverManager))


        osuWrapper = OsuWrapper(OsuDao(driverManager))

        newYearWrapper = NewYearWrapper(NewYearDao(driverManager))

        autoRemoveInactiveJoinMessageWrapper = AutoRemoveInactiveJoinMessageWrapper(AutoRemoveInactiveJoinMessageDao(driverManager))
        inactiveJMWrapper = InactiveJMWrapper(InactiveJMDao(driverManager))

        rateLimitWrapper = RatelimitWrapper(driverManager)

        validEmojiWrapper = ValidEmojiWrapper(ValidEmojiDao(driverManager))
        //After registering wrappers
        driverManager.executeTableRegistration()
        for (func in afterTableFunctions) {
            func()
        }
    }
}