package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.*
import me.melijn.melijnbot.objects.utils.*
import java.util.regex.Pattern

class PermissionCommand : AbstractCommand("command.permission") {

    init {
        id = 15
        name = "permission"
        aliases = arrayOf("perm", "p")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(UserCommand(), RoleCommand(), ChannelCommand())
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }


    class UserCommand : AbstractCommand("command.permission.user") {

        init {
            name = "user"
            aliases = arrayOf("u")
            children = arrayOf(SetCommand(), CopyCommand(this), ViewCommand(), ClearCommand())
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class SetCommand : AbstractCommand("command.permission.user.set") {

            init {
                name = "set"
                aliases = arrayOf("put", "s")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context, syntax)
                    return
                }

                val permissionNode = context.args[1]

                val state: PermState? = enumValueOrNull(context.args[2])
                if (state == null) {
                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, MESSAGE_UNKNOWN_PERMSTATE)
                        .replace(PLACEHOLDER_ARG, context.args[2])

                    sendMsg(context, msg)
                    return
                }

                val user = retrieveUserByArgsNMessage(context, 0) ?: return
                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val dao = context.daoManager.userPermissionWrapper
                if (permissions.size > 1) {
                    dao.setPermissions(context.getGuildId(), user.idLong, permissions, state)
                } else {
                    dao.setPermission(context.getGuildId(), user.idLong, permissions[0], state)
                }

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_USER, user.asTag)
                    .replace("%permissionNode%", permissionNode)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%state%", state.toString())

                sendMsg(context, msg)
            }
        }

        class ViewCommand : AbstractCommand("command.permission.user.view") {

            init {
                name = "view"
                aliases = arrayOf("v", "vw", "info")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val permissionNode = if (context.args.size > 1) context.args[1] else "*"
                val user = retrieveUserByArgsNMessage(context, 0) ?: return
                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val language = context.getLanguage()
                val title = i18n.getTranslation(language, "$root.response1.title")
                    .replace(PLACEHOLDER_USER, user.asTag)
                    .replace("%permissionNode%", permissionNode)

                var content = "\n```INI"
                val dao = context.daoManager.userPermissionWrapper.guildUserPermissionCache
                    .get(Pair(context.getGuildId(), user.idLong)).await()
                var index = 1
                for (perm in permissions) {
                    val state = dao.getOrDefault(perm, PermState.DEFAULT)
                    if (state != PermState.DEFAULT)
                        content += "\n${index++} - [$perm] - $state"
                }
                content += "```"

                sendMsgCodeBlock(context, title + content, "INI")
            }
        }

        class ClearCommand : AbstractCommand("command.permission.user.clear") {

            init {
                name = "clear"
                aliases = arrayOf("clr")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val user = retrieveUserByArgsNMessage(context, 0) ?: return

                context.daoManager.userPermissionWrapper.clear(context.getGuildId(), user.idLong)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_USER, user.asTag)
                sendMsg(context, msg)
            }
        }
    }

    class RoleCommand : AbstractCommand("command.permission.role") {

        init {
            name = "role"
            aliases = arrayOf("r")
            children = arrayOf(SetCommand(), CopyCommand(this), ViewCommand(), ClearCommand())
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class SetCommand : AbstractCommand("command.permission.role.set") {

            init {
                name = "set"
                aliases = arrayOf("put", "s")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context, syntax)
                    return
                }

                val permissionNode = context.args[1]

                val state: PermState? = enumValueOrNull(context.args[2])
                if (state == null) {
                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, MESSAGE_UNKNOWN_PERMSTATE)
                        .replace(PLACEHOLDER_ARG, context.args[2])
                    sendMsg(context, msg)

                    return
                }

                val role = getRoleByArgsNMessage(context, 0) ?: return

                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val dao = context.daoManager.rolePermissionWrapper
                if (permissions.size > 1) {
                    dao.setPermissions(context.getGuildId(), role.idLong, permissions, state)
                } else {
                    dao.setPermission(context.getGuildId(), role.idLong, permissions[0], state)
                }

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_ROLE, role.name)
                    .replace("%permissionNode%", permissionNode)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%state%", state.toString())

                sendMsg(context, msg)
            }

        }

        class ViewCommand : AbstractCommand("command.permission.role.view") {

            init {
                name = "view"
                aliases = arrayOf("v", "vw")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val permissionNode = if (context.args.size > 1) context.args[1] else "*"
                val role = getRoleByArgsNMessage(context, 0) ?: return
                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val language = context.getLanguage()
                val title = i18n.getTranslation(language, "$root.response1.title")
                    .replace(PLACEHOLDER_ROLE, role.name)
                    .replace("%permissionNode%", permissionNode)

                var content = "\n```INI"
                val dao = context.daoManager.rolePermissionWrapper.rolePermissionCache
                    .get(role.idLong).await()
                var index = 1
                for (perm in permissions) {
                    val state = dao.getOrDefault(perm, PermState.DEFAULT)
                    if (state != PermState.DEFAULT)
                        content += "\n${index++} - [$perm] - $state"
                }
                content += "```"

                sendMsgCodeBlock(context, title + content, "INI")
            }

        }

        class ClearCommand : AbstractCommand("command.permission.role.clear") {

            init {
                name = "clear"
                aliases = arrayOf("clr")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val role = getRoleByArgsNMessage(context, 0) ?: return

                context.daoManager.rolePermissionWrapper.clear(role.idLong)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_ROLE, role.name)
                sendMsg(context, msg)
            }

        }
    }

    class ChannelCommand : AbstractCommand("command.permission.channel") {

        init {
            name = "channel"
            aliases = arrayOf("c")
            children = arrayOf(UserChannelCommand(), RoleChannelCommand())
        }

        override suspend fun execute(context: CommandContext) {
            sendMsg(context, "Channel Permissions")
        }

        class RoleChannelCommand : AbstractCommand("command.permission.channel.role") {

            init {
                name = "role"
                aliases = arrayOf("r")
                children = arrayOf(SetCommand(), CopyCommand(this), ViewCommand(), ClearCommand())
            }

            override suspend fun execute(context: CommandContext) {
                sendMsg(context, "Role Channel Permissions")
            }

            class SetCommand : AbstractCommand("command.permission.channel.role.set") {

                init {
                    name = "set"
                    aliases = arrayOf("put", "s")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 4) {
                        sendSyntax(context, syntax)
                        return
                    }

                    val permissionNode = context.args[2]

                    val state: PermState? = enumValueOrNull(context.args[3])
                    if (state == null) {
                        val language = context.getLanguage()
                        val msg = i18n.getTranslation(language, MESSAGE_UNKNOWN_PERMSTATE)
                            .replace(PLACEHOLDER_ARG, context.args[3])
                        sendMsg(context, msg)

                        return
                    }

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role = getRoleByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                    val dao = context.daoManager.channelRolePermissionWrapper
                    if (permissions.size > 1) {
                        dao.setPermissions(context.getGuildId(), channel.idLong, role.idLong, permissions, state)
                    } else {
                        dao.setPermission(context.getGuildId(), channel.idLong, role.idLong, permissions[0], state)
                    }

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace("%textChannel%", channel.asTag)
                        .replace(PLACEHOLDER_ROLE, role.name)
                        .replace("%permissionNode%", permissionNode)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%state%", state.toString())

                    sendMsg(context, msg)
                }

            }

            class ViewCommand : AbstractCommand("command.permission.channel.role.view") {

                init {
                    name = "view"
                    aliases = arrayOf("v", "vw")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context, syntax)
                        return
                    }

                    val permissionNode = if (context.args.size > 2) context.args[2] else "*"

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role = getRoleByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                    val language = context.getLanguage()
                    val title = i18n.getTranslation(language, "$root.response1.title")
                        .replace(PLACEHOLDER_CHANNEL, channel.asTag)
                        .replace(PLACEHOLDER_ROLE, role.name)
                        .replace("%permissionNode%", permissionNode)

                    val channelRolePermissionCache = context.daoManager.channelRolePermissionWrapper.channelRolePermissionCache
                    val channelRole = channelRolePermissionCache.get(Pair(channel.idLong, role.idLong)).await()

                    var content = "```INI"
                    for ((index, perm) in permissions.withIndex()) {
                        val state = channelRole.getOrDefault(perm, PermState.DEFAULT)
                        if (state != PermState.DEFAULT) {
                            content += "\n${index + 1} - [$perm] - $state"
                        }
                    }
                    content += "```"

                    val msg = title + content
                    sendMsgCodeBlock(context, msg, "INI")
                }

            }

            class ClearCommand : AbstractCommand("command.permission.channel.role.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("clr")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context, syntax)
                        return
                    }

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role = getRoleByArgsNMessage(context, 1) ?: return

                    context.daoManager.channelRolePermissionWrapper.clear(channel.idLong, role.idLong)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace(PLACEHOLDER_ROLE, role.name)
                        .replace(PLACEHOLDER_CHANNEL, channel.asTag)
                    sendMsg(context, msg)
                }

            }
        }

        class UserChannelCommand : AbstractCommand("command.permission.channel.user") {

            init {
                name = "user"
                aliases = arrayOf("u")
                children = arrayOf(SetCommand(), CopyCommand(this), ViewCommand(), ClearCommand())
            }

            override suspend fun execute(context: CommandContext) {
                sendMsg(context, "User Channel Permissions")
            }

            class SetCommand : AbstractCommand("command.permission.channel.user.set") {

                init {
                    name = "set"
                    aliases = arrayOf("put", "s")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 4) {
                        sendSyntax(context, syntax)
                        return
                    }

                    val permissionNode = context.args[2]

                    val state: PermState? = enumValueOrNull(context.args[3])
                    if (state == null) {
                        val language = context.getLanguage()
                        val msg = i18n.getTranslation(language, MESSAGE_UNKNOWN_PERMSTATE)
                            .replace(PLACEHOLDER_ARG, context.args[3])
                        sendMsg(context, msg)

                        return
                    }

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user = retrieveUserByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                    val dao = context.daoManager.channelUserPermissionWrapper
                    if (permissions.size > 1) {
                        dao.setPermissions(context.getGuildId(), channel.idLong, user.idLong, permissions, state)
                    } else {
                        dao.setPermission(context.getGuildId(), channel.idLong, user.idLong, permissions[0], state)
                    }

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace("%textChannel%", channel.asTag)
                        .replace(PLACEHOLDER_USER, user.asTag)
                        .replace("%permissionNode%", permissionNode)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%state%", state.toString())

                    sendMsg(context, msg)
                }

            }

            class ViewCommand : AbstractCommand("command.permission.channel.user.view") {

                init {
                    name = "view"
                    aliases = arrayOf("v", "vw")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context, syntax)
                        return
                    }

                    val permissionNode = if (context.args.size > 2) context.args[2] else "*"

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user = retrieveUserByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArg(context, permissionNode) ?: return

                    val language = context.getLanguage()
                    val title = i18n.getTranslation(language, "$root.response1.title")
                        .replace(PLACEHOLDER_CHANNEL, channel.asTag)
                        .replace(PLACEHOLDER_USER, user.asTag)
                        .replace("%permissionNode%", permissionNode)
                    val cache = context.daoManager.channelUserPermissionWrapper.channelUserPermissionCache
                    val channelUser = cache.get(Pair(channel.idLong, user.idLong)).await()

                    var content = "```INI"
                    for ((index, perm) in permissions.withIndex()) {
                        val state = channelUser.getOrDefault(perm, PermState.DEFAULT)
                        if (state != PermState.DEFAULT) {
                            content += "\n${index + 1} - [$perm] - $state"
                        }
                    }
                    content += "```"

                    sendMsgCodeBlock(context, title + content, "INI")
                }

            }

            class ClearCommand : AbstractCommand("command.permission.channel.user.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("clr")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context, syntax)
                        return
                    }

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user = retrieveUserByArgsNMessage(context, 1) ?: return

                    context.daoManager.channelUserPermissionWrapper.clear(channel.idLong, user.idLong)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace(PLACEHOLDER_USER, user.asTag)
                        .replace(PLACEHOLDER_CHANNEL, channel.asTag)
                    sendMsg(context, msg)
                }

            }
        }
    }

    class CopyCommand(parent: AbstractCommand) : AbstractCommand("${parent.root}.copy") {

        init {
            name = "copy"
            aliases = arrayOf("cp")
            children = arrayOf(UserCommand(parent, root), RoleCommand(parent, root), ChannelCommand(parent, root))
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class UserCommand(private val copyParent: AbstractCommand, copyRoot: String) : AbstractCommand("$copyRoot.user") {
            init {
                name = "user"
                aliases = arrayOf("u")
            }

            override suspend fun execute(context: CommandContext) {
                val extraArg = if (
                    copyParent is PermissionCommand.ChannelCommand.RoleChannelCommand ||
                    copyParent is PermissionCommand.ChannelCommand.UserChannelCommand
                ) 1 else 0
                if (context.args.size < (2 + extraArg)) {
                    sendSyntax(context, syntax)
                    return
                }
                when (copyParent) {
                    is PermissionCommand.ChannelCommand.RoleChannelCommand -> {
                        copyChannelRoleToUser(context)
                    }
                    is PermissionCommand.ChannelCommand.UserChannelCommand -> {
                        copyChannelUserToUser(context)
                    }
                    is PermissionCommand.RoleCommand -> {
                        copyRoleToUser(context)
                    }
                    is PermissionCommand.UserCommand -> {
                        copyUserToUser(context)
                    }
                }
            }

            private suspend fun copyUserToUser(context: CommandContext) {
                val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.userPermissionWrapper
                val permissions = daoWrapper1.guildUserPermissionCache
                    .get(Pair(context.getGuildId(), user1.idLong)).await()

                daoWrapper1.setPermissions(context.getGuildId(), user2.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace("%user1%", user1.asTag)
                    .replace("%user2%", user2.asTag)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }

            private suspend fun copyRoleToUser(context: CommandContext) {
                val role1 = getRoleByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.rolePermissionWrapper
                val permissions = daoWrapper1.rolePermissionCache
                    .get(role1.idLong).await()

                val daoWrapper2 = context.daoManager.userPermissionWrapper
                daoWrapper2.setPermissions(context.getGuildId(), user2.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_ROLE, role1.name)
                    .replace(PLACEHOLDER_USER, user2.name)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }

            private suspend fun copyChannelUserToUser(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                val permissions = daoWrapper1.channelUserPermissionCache
                    .get(Pair(channel1.idLong, user2.idLong)).await()

                val daoWrapper2 = context.daoManager.userPermissionWrapper
                daoWrapper2.setPermissions(context.getGuildId(), user3.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace("%user1%", user2.asTag)
                    .replace(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .replace("%user2%", user3.asTag)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }

            private suspend fun copyChannelRoleToUser(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return
                val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                val permissions = daoWrapper1.channelRolePermissionCache
                    .get(Pair(channel1.idLong, role2.idLong)).await()

                val daoWrapper2 = context.daoManager.userPermissionWrapper
                daoWrapper2.setPermissions(context.getGuildId(), user3.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_ROLE, role2.name)
                    .replace(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .replace(PLACEHOLDER_USER, user3.asTag)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }

        }

        class RoleCommand(private val copyParent: AbstractCommand, copyRoot: String) : AbstractCommand("$copyRoot.role") {

            init {
                name = "role"
                aliases = arrayOf("r")
            }

            override suspend fun execute(context: CommandContext) {
                val extraArg = if (
                    copyParent is PermissionCommand.ChannelCommand.RoleChannelCommand ||
                    copyParent is PermissionCommand.ChannelCommand.UserChannelCommand
                ) 1 else 0
                if (context.args.size < (2 + extraArg)) {
                    sendSyntax(context, syntax)
                    return
                }
                when (copyParent) {
                    is PermissionCommand.ChannelCommand.RoleChannelCommand -> {
                        copyChannelRoleToRole(context)
                    }
                    is PermissionCommand.ChannelCommand.UserChannelCommand -> {
                        copyChannelUserToRole(context)
                    }
                    is PermissionCommand.RoleCommand -> {
                        copyRoleToRole(context)
                    }
                    is PermissionCommand.UserCommand -> {
                        copyUserToRole(context)
                    }
                }
            }

            private suspend fun copyUserToRole(context: CommandContext) {
                val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.userPermissionWrapper
                val permissions = daoWrapper1.guildUserPermissionCache
                    .get(Pair(context.getGuildId(), user1.idLong)).await()

                val daoWrapper2 = context.daoManager.rolePermissionWrapper
                daoWrapper2.setPermissions(context.getGuildId(), role2.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_USER, user1.asTag)
                    .replace(PLACEHOLDER_ROLE, role2.name)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }

            private suspend fun copyRoleToRole(context: CommandContext) {
                val role1 = getRoleByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.rolePermissionWrapper
                val permissions = daoWrapper1.rolePermissionCache
                    .get(role1.idLong).await()

                daoWrapper1.setPermissions(context.getGuildId(), role2.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace("%role1%", role1.name)
                    .replace("%role2%", role2.name)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }

            private suspend fun copyChannelUserToRole(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                val role3 = getRoleByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                val permissions = daoWrapper1.channelUserPermissionCache
                    .get(Pair(channel1.idLong, user2.idLong)).await()

                val daoWrapper2 = context.daoManager.rolePermissionWrapper
                daoWrapper2.setPermissions(context.getGuildId(), role3.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace(PLACEHOLDER_USER, user2.asTag)
                    .replace(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .replace(PLACEHOLDER_ROLE, role3.name)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }

            private suspend fun copyChannelRoleToRole(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return
                val role3 = getRoleByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                val permissions = daoWrapper1.channelRolePermissionCache
                    .get(Pair(channel1.idLong, role2.idLong)).await()

                val daoWrapper2 = context.daoManager.rolePermissionWrapper
                daoWrapper2.setPermissions(context.getGuildId(), role3.idLong, permissions)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.response1")
                    .replace("%role1%", role2.name)
                    .replace(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .replace("%role2%", role3.name)
                    .replace("%permissionCount%", permissions.size.toString())
                    .replace("%s%", if (permissions.size > 1) "s" else "")

                sendMsg(context, msg)
            }
        }

        class ChannelCommand(copyParent: AbstractCommand, copyRoot: String) : AbstractCommand("$copyRoot.channel") {
            init {
                name = "channel"
                aliases = arrayOf("c")
                children = arrayOf(RoleChannelCommand(copyParent, root), UserChannelCommand(copyParent, root))
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context, syntax)
            }

            class RoleChannelCommand(private val copyParent: AbstractCommand, parentRoot: String) : AbstractCommand("$parentRoot.role") {

                init {
                    name = "role"
                    aliases = arrayOf("r")
                }

                override suspend fun execute(context: CommandContext) {
                    val extraArg = if (
                        copyParent is PermissionCommand.ChannelCommand.RoleChannelCommand ||
                        copyParent is PermissionCommand.ChannelCommand.UserChannelCommand
                    ) 1 else 0
                    if (context.args.size < (3 + extraArg)) {
                        sendSyntax(context, syntax)
                        return
                    }
                    when (copyParent) {
                        is PermissionCommand.ChannelCommand.RoleChannelCommand -> {
                            copyChannelRoleToChannelRole(context)
                        }
                        is PermissionCommand.ChannelCommand.UserChannelCommand -> {
                            copyChannelUserToChannelRole(context)
                        }
                        is PermissionCommand.RoleCommand -> {
                            copyRoleToChannelRole(context)
                        }
                        is PermissionCommand.UserCommand -> {
                            copyUserToChannelRole(context)
                        }
                    }
                }

                private suspend fun copyUserToChannelRole(context: CommandContext) {
                    val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val role3 = getRoleByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.userPermissionWrapper
                    val permissions = daoWrapper1.guildUserPermissionCache
                        .get(Pair(context.getGuildId(), user1.idLong)).await()

                    val daoWrapper2 = context.daoManager.channelRolePermissionWrapper
                    daoWrapper2.setPermissions(context.getGuildId(), channel2.idLong, role3.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace(PLACEHOLDER_USER, user1.asTag)
                        .replace(PLACEHOLDER_CHANNEL, channel2.asTag)
                        .replace(PLACEHOLDER_ROLE, role3.name)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }

                private suspend fun copyRoleToChannelRole(context: CommandContext) {
                    val role1 = getRoleByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val role3 = getRoleByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.rolePermissionWrapper
                    val permissions = daoWrapper1.rolePermissionCache
                        .get(role1.idLong).await()

                    val daoWrapper2 = context.daoManager.channelRolePermissionWrapper
                    daoWrapper2.setPermissions(context.getGuildId(), channel2.idLong, role3.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace("%role1%", role1.name)
                        .replace("%channel%", channel2.asTag)
                        .replace("%role2%", role3.name)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }

                private suspend fun copyChannelUserToChannelRole(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val role4 = getRoleByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                    val permissions = daoWrapper1.channelUserPermissionCache
                        .get(Pair(channel1.idLong, user2.idLong)).await()

                    val daoWrapper2 = context.daoManager.channelRolePermissionWrapper
                    daoWrapper2.setPermissions(context.getGuildId(), channel3.idLong, role4.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace(PLACEHOLDER_USER, user2.asTag)
                        .replace("%channel1%", channel1.asTag)
                        .replace("%channel2%", channel3.asTag)
                        .replace(PLACEHOLDER_ROLE, role4.name)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }

                private suspend fun copyChannelRoleToChannelRole(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role2 = getRoleByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val role4 = getRoleByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                    val permissions = daoWrapper1.channelRolePermissionCache
                        .get(Pair(channel1.idLong, role2.idLong)).await()

                    daoWrapper1.setPermissions(context.getGuildId(), channel3.idLong, role4.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace("%role1%", role2.name)
                        .replace("%channel1%", channel1.asTag)
                        .replace("%channel2%", channel3.asTag)
                        .replace("%role2%", role4.name)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }
            }

            class UserChannelCommand(private val copyParent: AbstractCommand, parentRoot: String) : AbstractCommand("$parentRoot.user") {

                init {
                    name = "user"
                    aliases = arrayOf("u")
                }

                override suspend fun execute(context: CommandContext) {
                    val extraArg = if (
                        copyParent is PermissionCommand.ChannelCommand.RoleChannelCommand ||
                        copyParent is PermissionCommand.ChannelCommand.UserChannelCommand
                    ) 1 else 0
                    if (context.args.size < (3 + extraArg)) {
                        sendSyntax(context, syntax)
                        return
                    }
                    when (copyParent) {
                        is PermissionCommand.ChannelCommand.RoleChannelCommand -> {
                            copyChannelRoleToChannelUser(context)
                        }
                        is PermissionCommand.ChannelCommand.UserChannelCommand -> {
                            copyChannelUserToChannelUser(context)
                        }
                        is PermissionCommand.RoleCommand -> {
                            copyRoleToChannelUser(context)
                        }
                        is PermissionCommand.UserCommand -> {
                            copyUserToChannelUser(context)
                        }
                    }
                }

                private suspend fun copyUserToChannelUser(context: CommandContext) {
                    val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.userPermissionWrapper
                    val permissions = daoWrapper1.guildUserPermissionCache
                        .get(Pair(context.getGuildId(), user1.idLong)).await()

                    val daoWrapper2 = context.daoManager.channelUserPermissionWrapper
                    daoWrapper2.setPermissions(context.getGuildId(), channel2.idLong, user3.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace("%user1%", user1.asTag)
                        .replace(PLACEHOLDER_CHANNEL, channel2.asTag)
                        .replace("%user2%", user3.asTag)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }

                private suspend fun copyRoleToChannelUser(context: CommandContext) {
                    val role1 = getRoleByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.rolePermissionWrapper
                    val permissions = daoWrapper1.rolePermissionCache
                        .get(role1.idLong).await()

                    val daoWrapper2 = context.daoManager.channelUserPermissionWrapper
                    daoWrapper2.setPermissions(context.getGuildId(), channel2.idLong, user3.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace(PLACEHOLDER_ROLE, role1.name)
                        .replace(PLACEHOLDER_CHANNEL, channel2.asTag)
                        .replace(PLACEHOLDER_USER, user3.asTag)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }

                private suspend fun copyChannelUserToChannelUser(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val user4 = retrieveUserByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                    val permissions = daoWrapper1.channelUserPermissionCache
                        .get(Pair(channel1.idLong, user2.idLong)).await()

                    daoWrapper1.setPermissions(context.getGuildId(), channel3.idLong, user4.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace("%user1%", user2.asTag)
                        .replace("%channel1%", channel1.asTag)
                        .replace("%channel2%", channel3.asTag)
                        .replace("%user2%", user4.asTag)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }

                private suspend fun copyChannelRoleToChannelUser(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role2 = getRoleByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val user4 = retrieveUserByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                    val permissions = daoWrapper1.channelRolePermissionCache
                        .get(Pair(channel1.idLong, role2.idLong)).await()

                    val daoWrapper2 = context.daoManager.channelUserPermissionWrapper
                    daoWrapper2.setPermissions(context.getGuildId(), channel3.idLong, user4.idLong, permissions)

                    val language = context.getLanguage()
                    val msg = i18n.getTranslation(language, "$root.response1")
                        .replace(PLACEHOLDER_ROLE, role2.name)
                        .replace("%channel1%", channel1.asTag)
                        .replace("%channel2%", channel3.asTag)
                        .replace(PLACEHOLDER_USER, user4.asTag)
                        .replace("%permissionCount%", permissions.size.toString())
                        .replace("%s%", if (permissions.size > 1) "s" else "")

                    sendMsg(context, msg)
                }
            }
        }
    }
}

suspend fun getPermissionsFromArgNMessage(context: CommandContext, arg: String): List<String>? {
    val permissions = getPermissionsFromArg(context, arg)
    if (permissions == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, MESSAGE_UNKNOWN_PERMISSIONNODE)
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }
    return permissions
}

fun getPermissionsFromArg(context: CommandContext, arg: String): List<String>? {
    val category: CommandCategory? = enumValueOrNull(arg)
    val permParts = arg.split(".")

    val commands = if (category == null) {
        if (arg == "*") {
            context.getCommands()
        } else context.getCommands().filter { command -> command.isCommandFor(permParts[0]) }
    } else {
        context.getCommands().filter { command -> command.commandCategory == category }
    }

    val regex: Regex = when {
        arg == "*" || category != null -> ".*".toRegex()
        permParts.last() == "*" -> (
            Pattern.quote(permParts.subList(0, permParts.size - 1)
                .joinToString(".")) + "(..*)?"
            ).toRegex()

        else -> Pattern.quote(arg).toRegex()
    }

    val perms = getPermissions(commands).filter { perm ->
        perm.matches(regex)
    }

    return if (perms.isEmpty()) null else perms
}

fun getPermissions(commands: Collection<AbstractCommand>, prefix: String = ""): List<String> {
    val permissionList = ArrayList<String>()
    commands.forEach { cmd ->
        permissionList.add(prefix + cmd.name)
        permissionList.addAll(getPermissions(cmd.children.toList(), prefix + cmd.name + "."))
    }
    return permissionList
}