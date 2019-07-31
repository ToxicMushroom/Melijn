package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.getUserByArgsN
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
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
            sendMsg(context, "User Permissions")
        }

        class SetCommand : AbstractCommand("command.permission.user.set") {

            init {
                name = "set"
                aliases = arrayOf("put", "s")
            }

            override fun execute(context: CommandContext) {
                val user = getUserByArgsN(context, 0)
                val arg = context.args[1]
                if (user == null) {
                    sendMsg(context, Translateable("message.unknown.user").string(context).replace("%arg%", context.args[0]))
                    return
                }

                val permissions: List<String>? = getPermissionsFromArg(context, arg)
                if (permissions == null) {
                    sendMsg(context, Translateable("message.unknown.permission").string(context).replace("%arg%", context.args[0]))
                    return
                }

//                val dao = context.daoManager.userPermissionWrapper
//                if (permissions.size > 1) {
//                    dao.addPermissions(user.idLong, permissions)
//                } else {
//                    dao.addPermission(user.idLong, permissions[0])
//                }

                sendMsg(context, "Set User Permissions " + permissions.joinToString())
            }

        }

        class ViewCommand : AbstractCommand("command.permission.user.view") {

            init {
                name = "view"
                aliases = arrayOf("v", "vw")
            }

            override fun execute(context: CommandContext) {
                sendMsg(context, "View User Permissions")
            }

        }

        class ClearCommand : AbstractCommand("command.permission.user.clear") {

            init {
                name = "clear"
                aliases = arrayOf("clr")
            }

            override fun execute(context: CommandContext) {
                sendMsg(context, "Clear User Permissions")
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
    var category: CommandCategory? = null
    try {
        category = CommandCategory.valueOf(arg)
    } catch (e: IllegalArgumentException) {
    }

    val permParts = arg.split(".")

    val commands = if (category == null) {
        if (arg == "*") {
            context.getCommands()
        } else context.getCommands().filter { command -> command.isCommandFor(permParts[0]) }
    } else {
        context.getCommands().filter { command -> command.commandCategory == category }
    }

    val regex: Regex = when {
        arg == "*" -> ".*".toRegex()
        permParts.last() == "*" -> (
                Pattern.quote(permParts.subList(0, permParts.size - 1)
                        .joinToString(".")) + "(..*)?"
                ).toRegex()

        else -> Pattern.quote(arg).toRegex()
    }

    val perms = getPermissions(commands).filter { perm ->
        perm.matches(regex)
    }

    return if (perms.isEmpty()) null else (perms + "```$regex```")
}

fun getPermissions(commands: Collection<AbstractCommand>, prefix: String = ""): List<String> {
    val permissionList = ArrayList<String>()
    commands.forEach { cmd ->
        permissionList.add(prefix + cmd.name)
        permissionList.addAll(getPermissions(cmd.children.toList(), prefix + cmd.name + "."))
    }
    return permissionList
}