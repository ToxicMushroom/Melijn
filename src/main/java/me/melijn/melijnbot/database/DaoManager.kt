package me.melijn.melijnbot.database

import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.database.commands.CommandDao
import me.melijn.melijnbot.database.commands.CommandWrapper
import me.melijn.melijnbot.database.cooldown.CommandChannelCooldownDao
import me.melijn.melijnbot.database.cooldown.CommandChannelCooldownWrapper
import me.melijn.melijnbot.database.cooldown.CommandCooldownDao
import me.melijn.melijnbot.database.cooldown.CommandCooldownWrapper
import me.melijn.melijnbot.database.disabled.DisabledChannelCommandDao
import me.melijn.melijnbot.database.disabled.DisabledChannelCommandWrapper
import me.melijn.melijnbot.database.disabled.DisabledCommandDao
import me.melijn.melijnbot.database.disabled.DisabledCommandWrapper
import me.melijn.melijnbot.database.language.GuildLanguageDao
import me.melijn.melijnbot.database.language.GuildLanguageWrapper
import me.melijn.melijnbot.database.language.UserLanguageDao
import me.melijn.melijnbot.database.language.UserLanguageWrapper
import me.melijn.melijnbot.database.permissions.*
import me.melijn.melijnbot.database.prefixes.GuildPrefixDao
import me.melijn.melijnbot.database.prefixes.GuildPrefixWrapper
import me.melijn.melijnbot.database.prefixes.UserPrefixDao
import me.melijn.melijnbot.database.prefixes.UserPrefixWrapper
import me.melijn.melijnbot.database.supporters.SupporterWrapper
import me.melijn.melijnbot.database.supporters.UserSupporterDao
import me.melijn.melijnbot.objects.threading.TaskManager

const val RAPIDLY_USED_CACHE = 1L
const val FREQUENTLY_USED_CACHE = 5L
const val IMPORTANT_CACHE = 10L

const val HUGE_CACHE = 500L
const val LARGE_CACHE = 200L
const val NORMAL_CACHE = 100L
const val SMALL_CACHE = 50L

class DaoManager(taskManager: TaskManager, mysqlSettings: Settings.MySQL) {

    val commandWrapper: CommandWrapper

    val guildLanguageWrapper: GuildLanguageWrapper
    val userLanguageWrapper: UserLanguageWrapper

    val rolePermissionWrapper: RolePermissionWrapper
    val userPermissionWrapper: UserPermissionWrapper
    val channelRolePermissionWrapper: ChannelRolePermissionWrapper
    val channelUserPermissionWrapper: ChannelUserPermissionWrapper

    val disabledCommandWrapper: DisabledCommandWrapper
    val disabledChannelCommandWrapper: DisabledChannelCommandWrapper

    val commandChannelCoolDownWrapper: CommandChannelCooldownWrapper
    val commandCooldownWrapper: CommandCooldownWrapper

    val guildPrefixWrapper: GuildPrefixWrapper
    val userPrefixWrapper: UserPrefixWrapper

    val supporterWrapper: SupporterWrapper


    init {
        val driverManager = DriverManager(mysqlSettings, taskManager.messageUtils)

        commandWrapper = CommandWrapper(taskManager, CommandDao(driverManager))

        guildLanguageWrapper = GuildLanguageWrapper(taskManager, GuildLanguageDao(driverManager))
        userLanguageWrapper = UserLanguageWrapper(taskManager, UserLanguageDao(driverManager))

        rolePermissionWrapper = RolePermissionWrapper(taskManager, RolePermissionDao(driverManager))
        userPermissionWrapper = UserPermissionWrapper(taskManager, UserPermissionDao(driverManager))
        channelRolePermissionWrapper = ChannelRolePermissionWrapper(taskManager, ChannelRolePermissionDao(driverManager))
        channelUserPermissionWrapper = ChannelUserPermissionWrapper(taskManager, ChannelUserPermissionDao(driverManager))

        disabledCommandWrapper = DisabledCommandWrapper(taskManager, DisabledCommandDao(driverManager))
        disabledChannelCommandWrapper = DisabledChannelCommandWrapper(taskManager, DisabledChannelCommandDao(driverManager))

        commandCooldownWrapper = CommandCooldownWrapper(taskManager, CommandCooldownDao(driverManager))
        commandChannelCoolDownWrapper = CommandChannelCooldownWrapper(taskManager, CommandChannelCooldownDao(driverManager))

        guildPrefixWrapper = GuildPrefixWrapper(taskManager, GuildPrefixDao(driverManager))
        userPrefixWrapper = UserPrefixWrapper(taskManager, UserPrefixDao(driverManager))

        supporterWrapper = SupporterWrapper(taskManager, UserSupporterDao(driverManager))


        //After registering wrappers
        driverManager.executeTableRegistration()
    }
}