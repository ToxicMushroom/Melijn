package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.utils.MarkdownSanitizer

class RolesCommand : AbstractCommand("command.roles") {

    init {
        id = 10
        name = "roles"
        aliases = arrayOf("roleList", "listRoles")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty() && !context.isFromGuild) {
            sendSyntax(context)
            return
        }

        val guild: Guild = if (context.args.isNotEmpty() && DISCORD_ID.matches(context.args[0])) {
            context.shardManager.getGuildById(context.args[0]) ?: context.guild
        } else {
            context.guild
        }

        val available = context.args.isNotEmpty() && context.args[context.args.size - 1] == "available"

        val title = context.getTranslation("$root.response1.title")
            .withVariable("serverName", guild.name)

        val selfRolesGrouped = TaskManager.taskValueAsync { context.daoManager.selfRoleWrapper.getMap(context.guildId) }
        val selfRoleGroups =
            TaskManager.taskValueAsync { context.daoManager.selfRoleGroupWrapper.getMap(context.guildId) }
        val availableMap = mutableMapOf<String, List<Role>>()
        var content = if (available) "```MARKDOWN" else "```INI"

        for ((index, role) in guild.roleCache.withIndex()) {
            if (available) {
                for ((groupName, selfRoles) in selfRolesGrouped.await()) {
                    if (selfRoleGroups.await().firstOrNull { it.groupName == groupName }?.isSelfRoleable != true) {
                        continue
                    }

                    for (selfRoleIndex in 0 until selfRoles.length()) {
                        val selfRole = selfRoles.getArray(selfRoleIndex)
                        val rolesArray = selfRole.getArray(2)
                        if (rolesArray.length() > 1) {
                            continue
                        }
                        for (i in 0 until rolesArray.length()) {
                            val chanceRole = rolesArray.getArray(i)
                            val roleId = chanceRole.getLong(1)
                            if (roleId == role.idLong) {
                                availableMap[groupName] = availableMap.getOrDefault(groupName, emptyList()) + role
                                break
                            }
                        }
                        continue
                    }
                }
            } else {
                content += "\n${index + 1} - [${MarkdownSanitizer.escape(role.name)}] - ${role.id}"
            }
        }

        for ((groupName, roles) in availableMap) {
            content += "\n# $groupName\n"
            for ((index, role) in roles.withIndex()) {
                content += "${index + 1}. ${role.name}\n"
            }
        }

        content += "```"

        val msg = title + content
        sendRspCodeBlock(context, msg, "INI", true)
    }
}