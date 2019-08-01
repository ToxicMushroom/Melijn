package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.PermState
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
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

    override fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }


    class UserCommand : AbstractCommand("command.permission.user") {

        init {
            name = "user"
            aliases = arrayOf("u")
            children = arrayOf(SetCommand(), CopyCommand(this), ViewCommand(), ClearCommand())
        }

        override fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class SetCommand : AbstractCommand("command.permission.user.set") {

            init {
                name = "set"
                aliases = arrayOf("put", "s")
            }

            override fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context, syntax)
                    return
                }

                val permissionNode = context.args[1]

                val state: PermState? = enumValueOrNull(context.args[2])
                if (state == null) {
                    sendMsg(context, Translateable("message.unknown.permstate").string(context)
                            .replace("%arg%", context.args[2]))
                    return
                }

                val user = getUserByArgsN(context, 0)
                if (user == null) {
                    sendMsg(context, Translateable("message.unknown.user").string(context)
                            .replace("%arg%", context.args[0]))
                    return
                }

                val permissions: List<String>? = getPermissionsFromArg(context, permissionNode)
                if (permissions == null) {
                    sendMsg(context, Translateable("message.unknown.permissionnode").string(context)
                            .replace("%arg%", context.args[1]))
                    return
                }

                val dao = context.daoManager.userPermissionWrapper
                if (permissions.size > 1) {
                    dao.setPermissions(context.guildId, user.idLong, permissions, state)
                } else {
                    dao.setPermission(context.guildId, user.idLong, permissions[0], state)
                }

                val msg = Translateable("$root.response1").string(context)
                        .replace("%user%", user.asTag)
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

            override fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val permissionNode = if (context.args.size > 1) context.args[1] else "*"

                val user = getUserByArgsN(context, 0)
                if (user == null) {
                    sendMsg(context, Translateable("message.unknown.user").string(context)
                            .replace("%arg%", context.args[0]))
                    return
                }

                val permissions: List<String>? = getPermissionsFromArg(context, permissionNode)
                if (permissions == null) {
                    sendMsg(context, Translateable("message.unknown.permissionnode").string(context)
                            .replace("%arg%", permissionNode))
                    return
                }

                val title = Translateable("$root.response1.title").string(context)
                        .replace("%user%", user.asTag)
                        .replace("%permissionNode%", permissionNode)

                var content = "\n```INI"
                val dao = context.daoManager.userPermissionWrapper.guildUserPermissionCache
                        .get(Pair(context.guildId, user.idLong)).get()
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

            override fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val user = getUserByArgsN(context, 0)
                if (user == null) {
                    sendMsg(context, Translateable("message.unknown.user").string(context)
                            .replace("%arg%", context.args[0]))
                    return
                }

                context.daoManager.userPermissionWrapper.clear(context.guildId, user.idLong)

                val msg = Translateable("$root.response1").string(context)
                        .replace("%user%", user.asTag)
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

        override fun execute(context: CommandContext) {
            sendMsg(context, "Role Permissions")
        }

        class SetCommand : AbstractCommand("command.permission.role.set") {

            init {
                name = "set"
                aliases = arrayOf("put", "s")
            }

            override fun execute(context: CommandContext) {
                sendMsg(context, "Set Role Permissions")
            }

        }

        class ViewCommand : AbstractCommand("command.permission.role.view") {

            init {
                name = "view"
                aliases = arrayOf("v", "vw")
            }

            override fun execute(context: CommandContext) {
                sendMsg(context, "View Role Permissions")
            }

        }

        class ClearCommand : AbstractCommand("command.permission.role.clear") {

            init {
                name = "clear"
                aliases = arrayOf("clr")
            }

            override fun execute(context: CommandContext) {
                sendMsg(context, "Clear Role Permissions")
            }

        }
    }

    class ChannelCommand : AbstractCommand("command.permission.channel") {

        init {
            name = "channel"
            aliases = arrayOf("c")
            children = arrayOf(UserChannelCommand(), RoleChannelCommand())
        }

        override fun execute(context: CommandContext) {
            sendMsg(context, "Channel Permissions")
        }

        class RoleChannelCommand : AbstractCommand("command.permission.channel.role") {

            init {
                name = "role"
                aliases = arrayOf("r")
                children = arrayOf(SetCommand(), CopyCommand(this), ViewCommand(), ClearCommand())
            }

            override fun execute(context: CommandContext) {
                sendMsg(context, "Role Channel Permissions")
            }

            class SetCommand : AbstractCommand("command.permission.channel.role.set") {

                init {
                    name = "set"
                    aliases = arrayOf("put", "s")
                }

                override fun execute(context: CommandContext) {
                    sendMsg(context, "Set Role Channel Permissions")
                }

            }

            class ViewCommand : AbstractCommand("command.permission.channel.role.view") {

                init {
                    name = "view"
                    aliases = arrayOf("v", "vw")
                }

                override fun execute(context: CommandContext) {
                    sendMsg(context, "View Role Channel Permissions")
                }

            }

            class ClearCommand : AbstractCommand("command.permission.channel.role.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("clr")
                }

                override fun execute(context: CommandContext) {
                    sendMsg(context, "Clear Role Channel Permissions")
                }

            }
        }

        class UserChannelCommand : AbstractCommand("command.permission.channel.user") {

            init {
                name = "user"
                aliases = arrayOf("u")
                children = arrayOf(SetCommand(), CopyCommand(this), ViewCommand(), ClearCommand())
            }

            override fun execute(context: CommandContext) {
                sendMsg(context, "User Channel Permissions")
            }

            class SetCommand : AbstractCommand("command.permission.channel.user.set") {

                init {
                    name = "set"
                    aliases = arrayOf("put", "s")
                }

                override fun execute(context: CommandContext) {
                    sendMsg(context, "Set User Channel Permissions")
                }

            }

            class ViewCommand : AbstractCommand("command.permission.channel.user.view") {

                init {
                    name = "view"
                    aliases = arrayOf("v", "vw")
                }

                override fun execute(context: CommandContext) {
                    sendMsg(context, "View User Channel Permissions")
                }

            }

            class ClearCommand : AbstractCommand("command.permission.channel.user.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("clr")
                }

                override fun execute(context: CommandContext) {
                    sendMsg(context, "Clear User Channel Permissions")
                }
            }
        }
    }

    class CopyCommand(private val parent: AbstractCommand) : AbstractCommand("${parent.root}.copy") {

        init {
            name = "copy"
            aliases = arrayOf("cp")
        }

        override fun execute(context: CommandContext) {
            val part = when (parent) {
                is ChannelCommand.UserChannelCommand -> "User Channel"
                is ChannelCommand.RoleChannelCommand -> "Role Channel"
                is RoleCommand -> "Role"
                is UserCommand -> "User"
                else -> "Error"
            }
            sendMsg(context, "Copy $part Permissions")
        }
    }
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