package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.role.JoinRoleGroupInfo
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.utils.*
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
                        .replace("%group%", group.groupName)
                    sendMsg(context, msg)
                    return
                }

                val newState = getBooleanFromArgNMessage(context, 1) ?: return
                group.getAllRoles = newState
                context.daoManager.joinRoleGroupWrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.$newState")
                    .replace("%group%", group.groupName)
                sendMsg(context, msg)
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
                        .replace("%group%", group.groupName)
                    sendMsg(context, msg)
                    return
                }

                val newState = getBooleanFromArgNMessage(context, 1) ?: return
                group.isEnabled = newState
                context.daoManager.joinRoleGroupWrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.$newState")
                    .replace("%group%", group.groupName)
                sendMsg(context, msg)
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

                val sr = getSelfRoleGroupByGroupNameN(context, name)
                if (sr != null) {
                    val msg = context.getTranslation("$parent.exists")
                        .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }

                val newJr = JoinRoleGroupInfo(name, true, true)
                wrapper.insertOrUpdate(context.guildId, newJr)

                val msg = context.getTranslation("$root.added")
                    .replace("%group%", name)
                sendMsg(context, msg)
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
                    .replace("%group%", name)
                sendMsg(context, msg)
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
                val list = wrapper.joinRoleGroupCache[context.guildId].await().sortedBy { (groupName) ->
                    groupName
                }
                val index = getIntegerFromArgNMessage(context, 0, 1, list.size)
                    ?: return

                val group = list[index]

                wrapper.delete(context.guildId, group.groupName)

                val msg = context.getTranslation("$root.removed")
                    .replace("%group%", group.groupName)
                    .replace("%index%", "$index")

                sendMsg(context, msg)
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val wrapper = context.daoManager.joinRoleGroupWrapper
                val list = wrapper.joinRoleGroupCache[context.guildId].await().sortedBy { (groupName) ->
                    groupName
                }

                if (list.isEmpty()) {
                    val msg = context.getTranslation("$root.empty")
                    sendMsg(context, msg)
                    return
                }

                val title = context.getTranslation("$root.title")
                var content = "```INI\n[index] - [group] - [getAllRoles] - [enabled]"

                for ((index, roleInfo) in list.withIndex()) {
                    content += "${index + 1} - [${roleInfo.groupName}] - ${roleInfo.getAllRoles} - ${roleInfo.isEnabled}"
                }

                content += "```"
                content = title + content

                sendMsg(context, content)
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

                context.getTranslation("$root.added.$extra.chance")
                    .replace("%group%", group.groupName)
                    .replace(PLACEHOLDER_ROLE, role?.name ?: "kek")
                    .replace("%chance%", "$chance")
            } else {
                context.daoManager.joinRoleWrapper.set(context.guildId, group.groupName, role?.idLong ?: -1, 100)

                context.getTranslation("$root.added.$extra")
                    .replace("%group%", group.groupName)
                    .replace(PLACEHOLDER_ROLE, role?.name ?: "kek")
            }

            sendMsg(context, msg)
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
                    .replace("%group%", group.groupName)
                    .replace("%role%", role?.name ?: "kek")
            } else {
                context.getTranslation("$root.noentry.$extra")
                    .replace("%group%", group.groupName)
                    .replace("%role%", role?.name ?: "kek")
            }

            sendMsg(context, msg)
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

            val jrInfo = context.daoManager.joinRoleWrapper.joinRoleCache.get(context.guildId).await()
            val map = jrInfo.dataMap.toMutableMap()
            val ls = map[group.groupName]?.toMutableList()
            if (ls == null) {
                val msg = context.getTranslation("$root.emptygroup")
                    .replace("%group%", group.groupName)
                sendMsg(context, msg)
                return
            }
            val index = getIntegerFromArgNMessage(context, 1, 1, ls.size) ?: return
            val entry = ls[index]
            ls.removeAt(index)
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
                .replace("%group%", group.groupName)
                .replace("%index%", "$index")
                .replace("%role%", role?.name ?: "${entry.roleId}")

            sendMsg(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.joinRoleWrapper
            val map = wrapper.joinRoleCache[context.guildId].await().dataMap

            if (map.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendMsg(context, msg)
                return
            }

            val title = context.getTranslation("$root.title")
            val content = StringBuilder("```ini\n[group]:\n [index] - [role] - [roleId] - [chance]")

            for ((group, list) in map) {
                content.append("\n${group}:")
                for ((index, roleInfo) in list.sortedBy { it.roleId }.withIndex()) {
                    val role = roleInfo.roleId?.let { context.guild.getRoleById(it) }
                    content.append(" ${index + 1} - [${role?.name ?: "/"}] - ${roleInfo.roleId ?: -1} - ${roleInfo.chance}")
                }
            }

            content.append("```")
            val msg = title + content.toString()

            sendMsg(context, msg)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}

suspend fun getJoinRoleGroupByGroupNameN(context: CommandContext, group: String): JoinRoleGroupInfo? {
    val wrapper = context.daoManager.joinRoleGroupWrapper
    return wrapper.joinRoleGroupCache[context.guildId].await().firstOrNull { (groupName) ->
        groupName == group
    }
}

suspend fun getJoinRoleGroupByArgN(context: CommandContext, index: Int): JoinRoleGroupInfo? {
    val group = getStringFromArgsNMessage(context, index, 1, 64)
        ?: return null
    return getJoinRoleGroupByGroupNameN(context, group)
}

suspend fun getJoinRoleGroupByArgNMessage(context: CommandContext, index: Int): JoinRoleGroupInfo? {
    val wrapper = context.daoManager.joinRoleGroupWrapper
    val group = getStringFromArgsNMessage(context, index, 1, 64)
        ?: return null
    val joinRoleGroupInfo = wrapper.joinRoleGroupCache[context.guildId].await().firstOrNull { (groupName) ->
        groupName == group
    }
    if (joinRoleGroupInfo == null) {
        val msg = context.getTranslation("message.unknown.joinrolegroup")
            .replace(PLACEHOLDER_ARG, group)
        sendMsg(context, msg)
    }
    return joinRoleGroupInfo
}