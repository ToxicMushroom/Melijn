package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.escapeCodeblockMarkdown
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.withSafeVariable
import net.dv8tion.jda.api.entities.Role

class SelfRolesCommand : AbstractCommand("command.selfroles") {

    init {
        name = "selfRoles"
        aliases = arrayOf("srs")
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(context: ICommandContext) {
        val guild = context.guild
        val title = context.getTranslation("$root.response1.title")
            .withSafeVariable("serverName", guild.name)

        val daoManager = context.daoManager
        val selfRolesGrouped = TaskManager.taskValueAsync { daoManager.selfRoleWrapper.getMap(guild.idLong) }
        val selfRoleGroups = TaskManager.taskValueAsync { daoManager.selfRoleGroupWrapper.getMap(guild.idLong) }
        val availableMap = mutableMapOf<String, List<Role>>()
        for (role in guild.roleCache) {
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
        }

        var content = "```MARKDOWN"
        for ((groupName, roles) in availableMap) {
            content += "\n# $groupName\n"
            for ((index, role) in roles.withIndex()) {
                content += "${index + 1}. ${role.name.escapeCodeblockMarkdown(true)}\n"
            }
        }
        content += "```"

        val msg = title + content
        sendRspCodeBlock(context, msg, "MARKDOWN", true)
    }

}