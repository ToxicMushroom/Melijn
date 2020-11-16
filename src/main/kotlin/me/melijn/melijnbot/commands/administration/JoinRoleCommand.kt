package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.role.JoinRoleGroupInfo
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.Role

class JoinRoleCommand : AbstractCommand("command.joinrole") {

    init {
        id = 157
        name = "joinRole"
        aliases = arrayOf("jr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            ListArg(root),
            GroupArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class GroupArg(parent: String) : AbstractCommand("$parent.group") {

        init {
            name = "group"
            aliases = arrayOf("g", "gr", "gp")
            children = arrayOf(
                AddArg(root),
                RemoveArg(root),
                RemoveAtArg(root),
                ListArg(root),
                SetEnabledArg(root),
                SetGetAllRolesArg(root)
            )
        }

        class SetGetAllRolesArg(parent: String) : AbstractCommand("$parent.setgetallroles") {

            init {
                name = "setGetAllRoles"
                aliases = arrayOf("sgar")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getJoinRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size < 2) {
                    val msg = context.getTranslation("$root.show.${group.getAllRoles}")
                        .withVariable("group", group.groupName)
                    sendRsp(context, msg)
                    return
                }

                val newState = getBooleanFromArgNMessage(context, 1) ?: return
                group.getAllRoles = newState
                context.daoManager.joinRoleGroupWrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set.$newState")
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
            }
        }

        class SetEnabledArg(parent: String) : AbstractCommand("$parent.setenabled") {

            init {
                name = "setEnabled"
                aliases = arrayOf("se")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getJoinRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size < 2) {
                    val msg = context.getTranslation("$root.show.${group.isEnabled}")
                        .withVariable("group", group.groupName)
                    sendRsp(context, msg)
                    return
                }

                val newState = getBooleanFromArgNMessage(context, 1) ?: return
                group.isEnabled = newState
                context.daoManager.joinRoleGroupWrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set.$newState")
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
            }
        }

        class AddArg(val parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: CommandContext) {
                val name = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
                val wrapper = context.daoManager.joinRoleGroupWrapper

                val sr = getJoinRoleGroupByGroupNameN(context, name)
                if (sr != null) {
                    val msg = context.getTranslation("$parent.exists")
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                        .withVariable(PLACEHOLDER_ARG, name)
                    sendRsp(context, msg)
                    return
                }

                val newJr = JoinRoleGroupInfo(name, true, true)
                wrapper.insertOrUpdate(context.guildId, newJr)

                val msg = context.getTranslation("$root.added")
                    .withVariable("group", name)
                sendRsp(context, msg)
            }
        }


        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm", "r")
            }

            override suspend fun execute(context: CommandContext) {
                val joinRoleGroupInfo = getJoinRoleGroupByArgNMessage(context, 0) ?: return
                val wrapper = context.daoManager.joinRoleGroupWrapper
                wrapper.delete(context.guildId, joinRoleGroupInfo.groupName)

                val msg = context.getTranslation("$root.removed")
                    .withVariable("group", name)
                sendRsp(context, msg)
            }
        }


        class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

            init {
                name = "removeAt"
                aliases = arrayOf("rma", "ra")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val wrapper = context.daoManager.joinRoleGroupWrapper
                val list = wrapper.getList(context.guildId).sortedBy { (groupName) ->
                    groupName
                }
                val index = (getIntegerFromArgNMessage(context, 0, 1, list.size) ?: return) - 1

                val group = list[index]

                wrapper.delete(context.guildId, group.groupName)

                val msg = context.getTranslation("$root.removed")
                    .withVariable("group", group.groupName)
                    .withVariable("index", "$index")

                sendRsp(context, msg)
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val wrapper = context.daoManager.joinRoleGroupWrapper
                val list = wrapper.getList(context.guildId).sortedBy { (groupName) ->
                    groupName
                }

                if (list.isEmpty()) {
                    val msg = context.getTranslation("$root.empty")
                    sendRsp(context, msg)
                    return
                }

                val title = context.getTranslation("$root.title")
                var content = "```INI\n[index] - [group] - [getAllRoles] - [enabled]"

                for ((index, roleInfo) in list.withIndex()) {
                    content += "\n${index + 1} - [${roleInfo.groupName}] - ${roleInfo.getAllRoles} - ${roleInfo.isEnabled}"
                }

                content += "```"
                content = title + content

                sendRsp(context, content)
            }
        }


        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }
    }


    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val group = getJoinRoleGroupByArgNMessage(context, 0) ?: return

            val role: Role? = if (context.args[1] == "null") null else getRoleByArgsNMessage(context, 1) ?: return
            val extra = if (role == null) "null" else "role"

            val msg = if (context.args.size > 2) {
                val chance = getIntegerFromArgNMessage(context, 2, 1) ?: return

                context.daoManager.joinRoleWrapper.set(context.guildId, group.groupName, role?.idLong ?: -1, chance)

                context.getTranslation("$root.added.chance.$extra")
                    .withVariable("group", group.groupName)
                    .withVariable(PLACEHOLDER_ROLE, role?.name ?: "kek")
                    .withVariable("chance", "$chance")
            } else {
                context.daoManager.joinRoleWrapper.set(context.guildId, group.groupName, role?.idLong ?: -1, 100)

                context.getTranslation("$root.added.$extra")
                    .withVariable("group", group.groupName)
                    .withVariable(PLACEHOLDER_ROLE, role?.name ?: "kek")
            }

            sendRsp(context, msg)
        }
    }


    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val group = getJoinRoleGroupByArgNMessage(context, 0) ?: return

            val role: Role? = if (context.args[1] == "null") null else getRoleByArgsNMessage(context, 1) ?: return
            val extra = if (role == null) "null" else "role"

            val existed = context.daoManager.joinRoleWrapper.remove(context.guildId, group.groupName, role?.idLong)
            val msg = if (existed) {
                context.getTranslation("$root.removed.$extra")
                    .withVariable("group", group.groupName)
                    .withVariable("role", role?.name ?: "kek")
            } else {
                context.getTranslation("$root.noentry.$extra")
                    .withVariable("group", group.groupName)
                    .withVariable("role", role?.name ?: "kek")
            }

            sendRsp(context, msg)
        }
    }


    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "ra")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)

                return
            }

            val group = getJoinRoleGroupByArgNMessage(context, 0) ?: return

            val jrInfo = context.daoManager.joinRoleWrapper.getJRI(context.guildId)
            val map = jrInfo.dataMap.toMutableMap()
            val ls = map[group.groupName]?.toMutableList()
            if (ls == null) {
                val msg = context.getTranslation("$root.emptygroup")
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
                return
            }
            val index = getIntegerFromArgNMessage(context, 1, 1, ls.size) ?: return
            val entry = ls[index - 1]
            ls.removeAt(index - 1)
            if (ls.isNotEmpty()) {
                map[group.groupName] = ls
            } else {
                map.remove(group.groupName)
            }
            jrInfo.dataMap = map
            context.daoManager.joinRoleWrapper.put(context.guildId, jrInfo)

            val role = entry.roleId?.let { context.guild.getRoleById(it) }
            val extra = if (entry.roleId == null) "null" else "role"
            val msg = context.getTranslation("$root.removed.$extra")
                .withVariable("group", group.groupName)
                .withVariable("index", "$index")
                .withVariable("role", role?.name ?: "${entry.roleId}")

            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.joinRoleWrapper
            val map = wrapper.getJRI(context.guildId).dataMap

            if (map.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendRsp(context, msg)
                return
            }

            val title = context.getTranslation("$root.title")
            val content = StringBuilder("```ini\n[group]:\n [index] - [role] - [roleId] - [chance]")

            for ((group, list) in map) {
                content.append("\n${group}:")
                for ((index, roleInfo) in list.sortedBy { it.roleId }.withIndex()) {
                    val role = roleInfo.roleId?.let { context.guild.getRoleById(it) }
                    content.append("\n ${index + 1} - [${role?.name ?: "/"}] - ${roleInfo.roleId ?: -1} - ${roleInfo.chance}")
                }
            }

            content.append("```")
            val msg = title + content.toString()

            sendRsp(context, msg)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}

suspend fun getJoinRoleGroupByGroupNameN(context: CommandContext, group: String): JoinRoleGroupInfo? {
    val wrapper = context.daoManager.joinRoleGroupWrapper
    return wrapper.getList(context.guildId).firstOrNull { (groupName) ->
        groupName == group
    }
}

suspend fun getJoinRoleGroupByArgNMessage(context: CommandContext, index: Int): JoinRoleGroupInfo? {
    val wrapper = context.daoManager.joinRoleGroupWrapper
    val group = getStringFromArgsNMessage(context, index, 1, 64)
        ?: return null
    val joinRoleGroupInfo = wrapper.getList(context.guildId).firstOrNull { (groupName) ->
        groupName == group
    }
    if (joinRoleGroupInfo == null) {
        val msg = context.getTranslation("message.unknown.joinrolegroup")
            .withVariable(PLACEHOLDER_ARG, group)
        sendRsp(context, msg)
    }
    return joinRoleGroupInfo
}