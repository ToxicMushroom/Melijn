package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import java.util.regex.Pattern

class PermissionCommand : AbstractCommand("command.permission") {

    init {
        id = 15
        name = "permission"
        aliases = arrayOf("perm")
        children = arrayOf(
            UserArg(root),
            RoleArg(root),
            ChannelArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }


    class UserArg(parent: String) : AbstractCommand("$parent.user") {

        init {
            name = "user"
            aliases = arrayOf("u")
            children = arrayOf(
                SetCommand(root),
                CopyArg(this),
                ViewCommand(root),
                ClearCommand(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class SetCommand(parent: String) : AbstractCommand("$parent.set") {

            init {
                name = "set"
                aliases = arrayOf("put", "s")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context)
                    return
                }

                val permissionNode = context.args[1]

                val state: PermState = getEnumFromArgNMessage(context, 2, MESSAGE_UNKNOWN_PERMSTATE) ?: return

                val user = retrieveUserByArgsNMessage(context, 0) ?: return
                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val dao = context.daoManager.userPermissionWrapper
                if (permissions.size > 1) {
                    dao.setPermissions(context.guildId, user.idLong, permissions, state)
                } else {
                    dao.setPermission(context.guildId, user.idLong, permissions[0], state)
                }

                val msg = context.getTranslation("$root.response1")
                    .withVariable(PLACEHOLDER_USER, user.asTag)
                    .withVariable("permissionNode", permissionNode)
                    .withVariable("permissionCount", permissions.size.toString())
                    .withVariable("state", state.toString())

                sendRsp(context, msg)
            }
        }

        class ViewCommand(parent: String) : AbstractCommand("$parent.view") {

            init {
                name = "view"
                aliases = arrayOf("v", "vw", "list", "info")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val permissionNode = if (context.args.size > 1) context.args[1] else "*"
                val user = retrieveUserByArgsNMessage(context, 0) ?: return
                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val title = context.getTranslation("$root.response1.title")
                    .withVariable(PLACEHOLDER_USER, user.asTag)
                    .withVariable("permissionNode", permissionNode)

                var content = "\n```INI"
                val dao = context.daoManager.userPermissionWrapper.getPermMap(context.guildId, user.idLong)
                var index = 1
                for (perm in permissions) {
                    val state = dao.getOrDefault(perm, PermState.DEFAULT)
                    if (state != PermState.DEFAULT)
                        content += "\n${index++} - [$perm] - $state"
                }
                content += "```"

                sendRspCodeBlock(context, title + content, "INI", true)
            }
        }

        class ClearCommand(parent: String) : AbstractCommand("$parent.clear") {

            init {
                name = "clear"
                aliases = arrayOf("clr")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val user = retrieveUserByArgsNMessage(context, 0) ?: return

                context.daoManager.userPermissionWrapper.clear(context.guildId, user.idLong)

                val msg = context.getTranslation("$root.response1")
                    .withVariable(PLACEHOLDER_USER, user.asTag)
                sendRsp(context, msg)
            }
        }
    }

    class RoleArg(parent: String) : AbstractCommand("$parent.role") {

        init {
            name = "role"
            aliases = arrayOf("r")
            children = arrayOf(
                SetArg(root),
                CopyArg(this),
                ViewArg(root),
                ClearArg(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class SetArg(parent: String) : AbstractCommand("$parent.set") {

            init {
                name = "set"
                aliases = arrayOf("put", "s")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context)
                    return
                }

                val permissionNode = context.args[1]


                val state: PermState = getEnumFromArgNMessage(context, 2, MESSAGE_UNKNOWN_PERMSTATE) ?: return

                val role = getRoleByArgsNMessage(context, 0) ?: return

                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val dao = context.daoManager.rolePermissionWrapper
                if (permissions.size > 1) {
                    dao.setPermissions(context.guildId, role.idLong, permissions, state)
                } else {
                    dao.setPermission(context.guildId, role.idLong, permissions[0], state)
                }

                val msg = context.getTranslation("$root.response1")
                    .withVariable(PLACEHOLDER_ROLE, role.name)
                    .withVariable("permissionNode", permissionNode)
                    .withVariable("permissionCount", permissions.size.toString())
                    .withVariable("state", state.toString())

                sendRsp(context, msg)
            }

        }

        class ViewArg(parent: String) : AbstractCommand("$parent.view") {

            init {
                name = "view"
                aliases = arrayOf("v", "vw", "list", "info")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val permissionNode = if (context.args.size > 1) context.args[1] else "*"
                val role = getRoleByArgsNMessage(context, 0) ?: return
                val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                val title = context.getTranslation("$root.response1.title")
                    .withVariable(PLACEHOLDER_ROLE, role.name)
                    .withVariable("permissionNode", permissionNode)

                var content = "\n```INI"
                val dao = context.daoManager.rolePermissionWrapper.getPermMap(role.idLong)
                var index = 1
                for (perm in permissions) {
                    val state = dao.getOrDefault(perm, PermState.DEFAULT)
                    if (state != PermState.DEFAULT)
                        content += "\n${index++} - [$perm] - $state"
                }
                content += "```"

                sendRspCodeBlock(context, title + content, "INI", true)
            }

        }

        class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

            init {
                name = "clear"
                aliases = arrayOf("clr")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val role = getRoleByArgsNMessage(context, 0) ?: return

                context.daoManager.rolePermissionWrapper.clear(role.idLong)

                val msg = context.getTranslation("$root.response1")
                    .withVariable(PLACEHOLDER_ROLE, role.name)
                sendRsp(context, msg)
            }

        }
    }

    class ChannelArg(parent: String) : AbstractCommand("$parent.channel") {

        init {
            name = "channel"
            aliases = arrayOf("c")
            children = arrayOf(
                UserChannelArg(root),
                RoleChannelArg(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendRsp(context, "Channel Permissions")
        }

        class RoleChannelArg(parent: String) : AbstractCommand("$parent.role") {

            init {
                name = "role"
                aliases = arrayOf("r")
                children = arrayOf(
                    SetArg(root),
                    CopyArg(this),
                    ViewArg(root),
                    ClearArg(root)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class SetArg(parent: String) : AbstractCommand("$parent.set") {

                init {
                    name = "set"
                    aliases = arrayOf("put", "s")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 4) {
                        sendSyntax(context)
                        return
                    }

                    val permissionNode = context.args[2]

                    val state: PermState = getEnumFromArgNMessage(context, 3, MESSAGE_UNKNOWN_PERMSTATE) ?: return

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role = getRoleByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                    val dao = context.daoManager.channelRolePermissionWrapper
                    if (permissions.size > 1) {
                        dao.setPermissions(context.guildId, channel.idLong, role.idLong, permissions, state)
                    } else {
                        dao.setPermission(context.guildId, channel.idLong, role.idLong, permissions[0], state)
                    }

                    val msg = context.getTranslation("$root.response1")
                        .withVariable("textChannel", channel.asTag)
                        .withVariable(PLACEHOLDER_ROLE, role.name)
                        .withVariable("permissionNode", permissionNode)
                        .withVariable("permissionCount", permissions.size.toString())
                        .withVariable("state", state.toString())

                    sendRsp(context, msg)
                }

            }

            class ViewArg(parent: String) : AbstractCommand("$parent.view") {

                init {
                    name = "view"
                    aliases = arrayOf("v", "vw", "list", "info")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val permissionNode = if (context.args.size > 2) context.args[2] else "*"

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role = getRoleByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return


                    val title = context.getTranslation("$root.response1.title")
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                        .withVariable(PLACEHOLDER_ROLE, role.name)
                        .withVariable("permissionNode", permissionNode)


                    val channelRole = context.daoManager.channelRolePermissionWrapper.getPermMap(channel.idLong, role.idLong)

                    var content = "```INI"
                    for ((index, perm) in permissions.withIndex()) {
                        val state = channelRole.getOrDefault(perm, PermState.DEFAULT)
                        if (state != PermState.DEFAULT) {
                            content += "\n${index + 1} - [$perm] - $state"
                        }
                    }
                    content += "```"

                    val msg = title + content
                    sendRspCodeBlock(context, msg, "INI", true)
                }
            }

            class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("clr")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role = getRoleByArgsNMessage(context, 1) ?: return

                    context.daoManager.channelRolePermissionWrapper.clear(channel.idLong, role.idLong)


                    val msg = context.getTranslation("$root.response1")
                        .withVariable(PLACEHOLDER_ROLE, role.name)
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                    sendRsp(context, msg)
                }
            }
        }

        class UserChannelArg(parent: String) : AbstractCommand("$parent.user") {

            init {
                name = "user"
                aliases = arrayOf("u")
                children = arrayOf(
                    SetArg(root),
                    CopyArg(this),
                    ViewArg(root),
                    ClearArg(root)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class SetArg(parent: String) : AbstractCommand("$parent.set") {

                init {
                    name = "set"
                    aliases = arrayOf("put", "s")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 4) {
                        sendSyntax(context)
                        return
                    }

                    val permissionNode = context.args[2]

                    val state: PermState = getEnumFromArgNMessage(context, 3, MESSAGE_UNKNOWN_PERMSTATE) ?: return

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user = retrieveUserByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArgNMessage(context, permissionNode) ?: return

                    val dao = context.daoManager.channelUserPermissionWrapper
                    if (permissions.size > 1) {
                        dao.setPermissions(context.guildId, channel.idLong, user.idLong, permissions, state)
                    } else {
                        dao.setPermission(context.guildId, channel.idLong, user.idLong, permissions[0], state)
                    }


                    val msg = context.getTranslation("$root.response1")
                        .withVariable("textChannel", channel.asTag)
                        .withVariable(PLACEHOLDER_USER, user.asTag)
                        .withVariable("permissionNode", permissionNode)
                        .withVariable("permissionCount", permissions.size.toString())
                        .withVariable("state", state.toString())

                    sendRsp(context, msg)
                }

            }

            class ViewArg(parent: String) : AbstractCommand("$parent.view") {

                init {
                    name = "view"
                    aliases = arrayOf("v", "vw", "list", "info")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val permissionNode = if (context.args.size > 2) context.args[2] else "*"

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user = retrieveUserByArgsNMessage(context, 1) ?: return
                    val permissions = getPermissionsFromArg(context, permissionNode) ?: return


                    val title = context.getTranslation("$root.response1.title")
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                        .withVariable(PLACEHOLDER_USER, user.asTag)
                        .withVariable("permissionNode", permissionNode)
                    val wrapper = context.daoManager.channelUserPermissionWrapper
                    val channelUser = wrapper.getPermMap(channel.idLong, user.idLong)

                    var content = "```INI"
                    for ((index, perm) in permissions.withIndex()) {
                        val state = channelUser.getOrDefault(perm, PermState.DEFAULT)
                        if (state != PermState.DEFAULT) {
                            content += "\n${index + 1} - [$perm] - $state"
                        }
                    }
                    content += "```"

                    sendRspCodeBlock(context, title + content, "INI", true)
                }

            }

            class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("clr")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user = retrieveUserByArgsNMessage(context, 1) ?: return

                    context.daoManager.channelUserPermissionWrapper.clear(channel.idLong, user.idLong)


                    val msg = context.getTranslation("$root.response1")
                        .withVariable(PLACEHOLDER_USER, user.asTag)
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                    sendRsp(context, msg)
                }

            }
        }
    }

    class CopyArg(parent: AbstractCommand) : AbstractCommand("${parent.root}.copy") {

        init {
            name = "copy"
            aliases = arrayOf("cp")
            children = arrayOf(
                UserArg(parent, root),
                RoleArg(parent, root),
                ChannelArg(parent, root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class UserArg(private val copyParent: AbstractCommand, copyRoot: String) : AbstractCommand("$copyRoot.user") {
            init {
                name = "user"
                aliases = arrayOf("u")
            }

            override suspend fun execute(context: CommandContext) {
                val extraArg = if (
                    copyParent is PermissionCommand.ChannelArg.RoleChannelArg ||
                    copyParent is PermissionCommand.ChannelArg.UserChannelArg
                ) 1 else 0
                if (context.args.size < (2 + extraArg)) {
                    sendSyntax(context)
                    return
                }
                when (copyParent) {
                    is PermissionCommand.ChannelArg.RoleChannelArg -> {
                        copyChannelRoleToUser(context)
                    }
                    is PermissionCommand.ChannelArg.UserChannelArg -> {
                        copyChannelUserToUser(context)
                    }
                    is PermissionCommand.RoleArg -> {
                        copyRoleToUser(context)
                    }
                    is PermissionCommand.UserArg -> {
                        copyUserToUser(context)
                    }
                }
            }

            private suspend fun copyUserToUser(context: CommandContext) {
                val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.userPermissionWrapper
                val permissions = daoWrapper1.getPermMap(context.guildId, user1.idLong)

                daoWrapper1.setPermissions(context.guildId, user2.idLong, permissions)


                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }

                val msg = context.getTranslation(path)
                    .withVariable("user1", user1.asTag)
                    .withVariable("user2", user2.asTag)
                    .withVariable("permissionCount", permissions.size.toString())

                sendRsp(context, msg)
            }

            private suspend fun copyRoleToUser(context: CommandContext) {
                val role1 = getRoleByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.rolePermissionWrapper
                val permissions = daoWrapper1.getPermMap(role1.idLong)

                val daoWrapper2 = context.daoManager.userPermissionWrapper
                daoWrapper2.setPermissions(context.guildId, user2.idLong, permissions)


                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }
                val msg = context.getTranslation(path)
                    .withVariable(PLACEHOLDER_ROLE, role1.name)
                    .withVariable(PLACEHOLDER_USER, user2.name)
                    .withVariable("permissionCount", permissions.size.toString())

                sendRsp(context, msg)
            }

            private suspend fun copyChannelUserToUser(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                val permissions = daoWrapper1.getPermMap(channel1.idLong, user2.idLong)

                val daoWrapper2 = context.daoManager.userPermissionWrapper
                daoWrapper2.setPermissions(context.guildId, user3.idLong, permissions)


                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }

                val msg = context.getTranslation(path)
                    .withVariable("user1", user2.asTag)
                    .withVariable(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .withVariable("user2", user3.asTag)
                    .withVariable("permissionCount", permissions.size.toString())

                sendRsp(context, msg)
            }

            private suspend fun copyChannelRoleToUser(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return
                val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                val permissions = daoWrapper1.getPermMap(channel1.idLong, role2.idLong)

                val daoWrapper2 = context.daoManager.userPermissionWrapper
                daoWrapper2.setPermissions(context.guildId, user3.idLong, permissions)

                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }

                val msg = context.getTranslation(path)
                    .withVariable(PLACEHOLDER_ROLE, role2.name)
                    .withVariable(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .withVariable(PLACEHOLDER_USER, user3.asTag)
                    .withVariable("permissionCount", permissions.size.toString())

                sendRsp(context, msg)
            }

        }

        class RoleArg(private val copyParent: AbstractCommand, copyRoot: String) : AbstractCommand("$copyRoot.role") {

            init {
                name = "role"
                aliases = arrayOf("r")
            }

            override suspend fun execute(context: CommandContext) {
                val extraArg = if (
                    copyParent is PermissionCommand.ChannelArg.RoleChannelArg ||
                    copyParent is PermissionCommand.ChannelArg.UserChannelArg
                ) 1 else 0
                if (context.args.size < (2 + extraArg)) {
                    sendSyntax(context)
                    return
                }
                when (copyParent) {
                    is PermissionCommand.ChannelArg.RoleChannelArg -> {
                        copyChannelRoleToRole(context)
                    }
                    is PermissionCommand.ChannelArg.UserChannelArg -> {
                        copyChannelUserToRole(context)
                    }
                    is PermissionCommand.RoleArg -> {
                        copyRoleToRole(context)
                    }
                    is PermissionCommand.UserArg -> {
                        copyUserToRole(context)
                    }
                }
            }

            private suspend fun copyUserToRole(context: CommandContext) {
                val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.userPermissionWrapper
                val permissions = daoWrapper1.getPermMap(context.guildId, user1.idLong)

                val daoWrapper2 = context.daoManager.rolePermissionWrapper
                daoWrapper2.setPermissions(context.guildId, role2.idLong, permissions)


                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }
                val msg = context.getTranslation(path)
                    .withVariable(PLACEHOLDER_USER, user1.asTag)
                    .withVariable(PLACEHOLDER_ROLE, role2.name)
                    .withVariable("permissionCount", permissions.size.toString())


                sendRsp(context, msg)
            }

            private suspend fun copyRoleToRole(context: CommandContext) {
                val role1 = getRoleByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return

                val daoWrapper1 = context.daoManager.rolePermissionWrapper
                val permissions = daoWrapper1.getPermMap(role1.idLong)

                daoWrapper1.setPermissions(context.guildId, role2.idLong, permissions)
                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }

                val msg = context.getTranslation(path)
                    .withVariable("role1", role1.name)
                    .withVariable("role2", role2.name)
                    .withVariable("permissionCount", permissions.size.toString())


                sendRsp(context, msg)
            }

            private suspend fun copyChannelUserToRole(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                val role3 = getRoleByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                val permissions = daoWrapper1.getPermMap(channel1.idLong, user2.idLong)

                val daoWrapper2 = context.daoManager.rolePermissionWrapper
                daoWrapper2.setPermissions(context.guildId, role3.idLong, permissions)
                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }

                val msg = context.getTranslation(path)
                    .withVariable(PLACEHOLDER_USER, user2.asTag)
                    .withVariable(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .withVariable(PLACEHOLDER_ROLE, role3.name)
                    .withVariable("permissionCount", permissions.size.toString())


                sendRsp(context, msg)
            }

            private suspend fun copyChannelRoleToRole(context: CommandContext) {
                val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                val role2 = getRoleByArgsNMessage(context, 1) ?: return
                val role3 = getRoleByArgsNMessage(context, 2) ?: return

                val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                val permissions = daoWrapper1.getPermMap(channel1.idLong, role2.idLong)

                val daoWrapper2 = context.daoManager.rolePermissionWrapper
                daoWrapper2.setPermissions(context.guildId, role3.idLong, permissions)
                val path = "$root.response1" + if (permissions.size > 1) {
                    ".multiple"
                } else {
                    ""
                }

                val msg = context.getTranslation(path)
                    .withVariable("role1", role2.name)
                    .withVariable(PLACEHOLDER_CHANNEL, channel1.asTag)
                    .withVariable("role2", role3.name)
                    .withVariable("permissionCount", permissions.size.toString())


                sendRsp(context, msg)
            }
        }

        class ChannelArg(copyParent: AbstractCommand, copyRoot: String) : AbstractCommand("$copyRoot.channel") {
            init {
                name = "channel"
                aliases = arrayOf("c")
                children = arrayOf(RoleChannelCommand(copyParent, root), UserChannelCommand(copyParent, root))
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class RoleChannelCommand(private val copyParent: AbstractCommand, parentRoot: String) : AbstractCommand("$parentRoot.role") {

                init {
                    name = "role"
                    aliases = arrayOf("r")
                }

                override suspend fun execute(context: CommandContext) {
                    val extraArg = if (
                        copyParent is PermissionCommand.ChannelArg.RoleChannelArg ||
                        copyParent is PermissionCommand.ChannelArg.UserChannelArg
                    ) 1 else 0
                    if (context.args.size < (3 + extraArg)) {
                        sendSyntax(context)
                        return
                    }
                    when (copyParent) {
                        is PermissionCommand.ChannelArg.RoleChannelArg -> {
                            copyChannelRoleToChannelRole(context)
                        }
                        is PermissionCommand.ChannelArg.UserChannelArg -> {
                            copyChannelUserToChannelRole(context)
                        }
                        is PermissionCommand.RoleArg -> {
                            copyRoleToChannelRole(context)
                        }
                        is PermissionCommand.UserArg -> {
                            copyUserToChannelRole(context)
                        }
                    }
                }

                private suspend fun copyUserToChannelRole(context: CommandContext) {
                    val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val role3 = getRoleByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.userPermissionWrapper
                    val permissions = daoWrapper1.getPermMap(context.guildId, user1.idLong)

                    val daoWrapper2 = context.daoManager.channelRolePermissionWrapper
                    daoWrapper2.setPermissions(context.guildId, channel2.idLong, role3.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable(PLACEHOLDER_USER, user1.asTag)
                        .withVariable(PLACEHOLDER_CHANNEL, channel2.asTag)
                        .withVariable(PLACEHOLDER_ROLE, role3.name)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }

                private suspend fun copyRoleToChannelRole(context: CommandContext) {
                    val role1 = getRoleByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val role3 = getRoleByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.rolePermissionWrapper
                    val permissions = daoWrapper1.getPermMap(role1.idLong)

                    val daoWrapper2 = context.daoManager.channelRolePermissionWrapper
                    daoWrapper2.setPermissions(context.guildId, channel2.idLong, role3.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable("role1", role1.name)
                        .withVariable(PLACEHOLDER_CHANNEL, channel2.asTag)
                        .withVariable("role2", role3.name)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }

                private suspend fun copyChannelUserToChannelRole(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val role4 = getRoleByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                    val permissions = daoWrapper1.getPermMap(channel1.idLong, user2.idLong)

                    val daoWrapper2 = context.daoManager.channelRolePermissionWrapper
                    daoWrapper2.setPermissions(context.guildId, channel3.idLong, role4.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable(PLACEHOLDER_USER, user2.asTag)
                        .withVariable("channel1", channel1.asTag)
                        .withVariable("channel2", channel3.asTag)
                        .withVariable(PLACEHOLDER_ROLE, role4.name)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }

                private suspend fun copyChannelRoleToChannelRole(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role2 = getRoleByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val role4 = getRoleByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                    val permissions = daoWrapper1.getPermMap(channel1.idLong, role2.idLong)

                    daoWrapper1.setPermissions(context.guildId, channel3.idLong, role4.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable("role1", role2.name)
                        .withVariable("channel1", channel1.asTag)
                        .withVariable("channel2", channel3.asTag)
                        .withVariable("role2", role4.name)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }
            }

            class UserChannelCommand(private val copyParent: AbstractCommand, parentRoot: String) : AbstractCommand("$parentRoot.user") {

                init {
                    name = "user"
                    aliases = arrayOf("u")
                }

                override suspend fun execute(context: CommandContext) {
                    val extraArg = if (
                        copyParent is PermissionCommand.ChannelArg.RoleChannelArg ||
                        copyParent is PermissionCommand.ChannelArg.UserChannelArg
                    ) 1 else 0
                    if (context.args.size < (3 + extraArg)) {
                        sendSyntax(context)
                        return
                    }
                    when (copyParent) {
                        is PermissionCommand.ChannelArg.RoleChannelArg -> {
                            copyChannelRoleToChannelUser(context)
                        }
                        is PermissionCommand.ChannelArg.UserChannelArg -> {
                            copyChannelUserToChannelUser(context)
                        }
                        is PermissionCommand.RoleArg -> {
                            copyRoleToChannelUser(context)
                        }
                        is PermissionCommand.UserArg -> {
                            copyUserToChannelUser(context)
                        }
                    }
                }

                private suspend fun copyUserToChannelUser(context: CommandContext) {
                    val user1 = retrieveUserByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.userPermissionWrapper
                    val permissions = daoWrapper1.getPermMap(context.guildId, user1.idLong)

                    val daoWrapper2 = context.daoManager.channelUserPermissionWrapper
                    daoWrapper2.setPermissions(context.guildId, channel2.idLong, user3.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable("user1", user1.asTag)
                        .withVariable(PLACEHOLDER_CHANNEL, channel2.asTag)
                        .withVariable("user2", user3.asTag)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }

                private suspend fun copyRoleToChannelUser(context: CommandContext) {
                    val role1 = getRoleByArgsNMessage(context, 0) ?: return
                    val channel2 = getTextChannelByArgsNMessage(context, 1) ?: return
                    val user3 = retrieveUserByArgsNMessage(context, 2) ?: return

                    val daoWrapper1 = context.daoManager.rolePermissionWrapper
                    val permissions = daoWrapper1.getPermMap(role1.idLong)

                    val daoWrapper2 = context.daoManager.channelUserPermissionWrapper
                    daoWrapper2.setPermissions(context.guildId, channel2.idLong, user3.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable(PLACEHOLDER_ROLE, role1.name)
                        .withVariable(PLACEHOLDER_CHANNEL, channel2.asTag)
                        .withVariable(PLACEHOLDER_USER, user3.asTag)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }

                private suspend fun copyChannelUserToChannelUser(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val user2 = retrieveUserByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val user4 = retrieveUserByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelUserPermissionWrapper
                    val permissions = daoWrapper1.getPermMap(channel1.idLong, user2.idLong)

                    daoWrapper1.setPermissions(context.guildId, channel3.idLong, user4.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable("user1", user2.asTag)
                        .withVariable("channel1", channel1.asTag)
                        .withVariable("channel2", channel3.asTag)
                        .withVariable("user2", user4.asTag)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }

                private suspend fun copyChannelRoleToChannelUser(context: CommandContext) {
                    val channel1 = getTextChannelByArgsNMessage(context, 0) ?: return
                    val role2 = getRoleByArgsNMessage(context, 1) ?: return
                    val channel3 = getTextChannelByArgsNMessage(context, 2) ?: return
                    val user4 = retrieveUserByArgsNMessage(context, 3) ?: return

                    val daoWrapper1 = context.daoManager.channelRolePermissionWrapper
                    val permissions = daoWrapper1.getPermMap(channel1.idLong, role2.idLong)

                    val daoWrapper2 = context.daoManager.channelUserPermissionWrapper
                    daoWrapper2.setPermissions(context.guildId, channel3.idLong, user4.idLong, permissions)
                    val path = "$root.response1" + if (permissions.size > 1) {
                        ".multiple"
                    } else {
                        ""
                    }

                    val msg = context.getTranslation(path)
                        .withVariable(PLACEHOLDER_ROLE, role2.name)
                        .withVariable("channel1", channel1.asTag)
                        .withVariable("channel2", channel3.asTag)
                        .withVariable(PLACEHOLDER_USER, user4.asTag)
                        .withVariable("permissionCount", permissions.size.toString())


                    sendRsp(context, msg)
                }
            }
        }
    }
}

suspend fun getPermissionsFromArgNMessage(context: CommandContext, arg: String): List<String>? {
    val permissions = getPermissionsFromArg(context, arg)
    if (permissions == null) {

        val msg = context.getTranslation(MESSAGE_UNKNOWN_PERMISSIONNODE)
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }
    return permissions
}

fun getPermissionsFromArg(context: CommandContext, arg: String): List<String>? {
    val category: CommandCategory? = enumValueOrNull(arg)
    val permParts = arg.split(".")

    val commands = if (category == null) {
        if (arg == "*") {
            context.commandList
        } else context.commandList.filter { command -> command.isCommandFor(permParts[0]) }
    } else {
        context.commandList.filter { command -> command.commandCategory == category }
    }

    val regex: Regex = when {
        arg == "*" || category != null -> ".*".toRegex()
        permParts.last() == "*" -> (
            Pattern.quote(permParts.subList(0, permParts.size - 1)
                .joinToString(".")) + "(..*)?"
            ).toRegex()

        else -> Pattern.quote(arg).toRegex()
    }

    val perms = getPermissions(commands)
        .filter { perm ->
            perm.matches(regex)
        }.toMutableList()

    val extraNodes = SpecialPermission.values()
        .filter { perm -> regex.matches(perm.node) }
        .map { perm -> perm.node.toLowerCase() }
    perms.addAll(extraNodes)

    val matcher = ccTagPattern.matcher(arg)
    if (perms.isEmpty() && matcher.matches()) {
        perms.add(arg.toLowerCase())
    }

    return if (perms.isEmpty()) null else perms
}

fun getPermissions(commands: Collection<AbstractCommand>, prefix: String = ""): List<String> {
    val permissionList = ArrayList<String>()
    commands.forEach { cmd ->
        permissionList.add((prefix + cmd.name).toLowerCase())
        permissionList.addAll(getPermissions(cmd.children.toList(), (prefix + cmd.name).toLowerCase() + "."))
    }
    return permissionList
}