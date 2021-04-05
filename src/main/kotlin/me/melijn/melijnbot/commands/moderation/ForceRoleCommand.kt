package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class ForceRoleCommand : AbstractCommand("command.forcerole") {

    init {
        id = 38
        name = "forceRole"
        aliases = arrayOf("fr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ViewUserArg(root),
            ViewRoleArg(root),
            ListArg(root)
        )
        commandCategory = CommandCategory.MODERATION
    }

    class ViewRoleArg(parent: String) : AbstractCommand("$parent.viewrole") {

        init {
            name = "viewRole"
            aliases = arrayOf("roleInfo")
        }

        suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val role = getRoleByArgsNMessage(context, 0) ?: return

            val userIds = context.daoManager.forceRoleWrapper.getForceRoles(context.guildId)
                .filter { it.value.contains(role.idLong) }.keys

            var content = "```INI\n[user] - [userId]"
            for (userId in userIds) {
                content += if (userIds.size < 100) {
                    val member = context.shardManager.retrieveUserById(userId).awaitOrNull()
                    if (member == null) "\n- $userId"
                    else "\n- [%user%] - $userId".withSafeVariable("user", member.asTag)
                } else {
                    "\n- $userId"
                }
            }
            if (userIds.isEmpty()) content += "/"
            content += "```"

            val msg = context.getTranslation("$root.title")
                .withSafeVariable(PLACEHOLDER_ROLE, role.name) + content
            sendRspCodeBlock(context, msg, "INI", true)
        }
    }

    class ViewUserArg(parent: String) : AbstractCommand("$parent.viewuser") {

        init {
            name = "viewUser"
            aliases = arrayOf("userInfo")
        }

        suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val user = retrieveUserByArgsNMessage(context, 0) ?: return

            val roleIds = context.daoManager.forceRoleWrapper.getForceRoles(context.guildId)
                .getOrDefault(user.idLong, emptyList())

            var content = "```INI\n[role] - [roleId]"
            for (roleId in roleIds) {
                val role = context.guild.getRoleById(roleId)
                content += "\n- [${role?.name}] - $roleId"
            }
            if (roleIds.isEmpty()) content += "\n/"
            content += "```"

            val msg = context.getTranslation("$root.title")
                .withSafeVariable(PLACEHOLDER_USER, user.asTag) + content
            sendRspCodeBlock(context, msg, "INI", true)
        }
    }

    suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class AddArg(root: String) : AbstractCommand("$root.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1, sameGuildAsContext = true, canInteract = true) ?: return
            val member = context.guild.retrieveMember(user).awaitOrNull()
            if (member != null) {

                if (!context.guild.selfMember.canInteract(member)) {
                    val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                        .withSafeVariable(PLACEHOLDER_USER, member.asTag)
                    sendRsp(context, msg)
                    return
                }
                if (!context.member.canInteract(member) && !hasPermission(
                        context,
                        SpecialPermission.PUNISH_BYPASS_HIGHER.node,
                        true
                    )
                ) {
                    val msg = context.getTranslation(MESSAGE_INTERACT_MEMBER_HIARCHYEXCEPTION)
                        .withVariable(PLACEHOLDER_USER, member.asTag)
                    sendRsp(context, msg)
                    return
                }
            }

            context.daoManager.forceRoleWrapper.add(context.guildId, user.idLong, role.idLong)
            if (member != null && !member.roles.contains(role)) {
                if (!context.guild.addRoleToMember(member, role).reason("(forceRole add) ${context.author.asTag}")
                        .awaitBool()
                ) {
                    LogUtils.sendMessageFailedToAddRoleToMember(context.daoManager, member, role)
                }
            }

            val msg = context.getTranslation("$root.success")
                .withSafeVariable(PLACEHOLDER_USER, user.asTag)
                .withSafeVariable(PLACEHOLDER_ROLE, role.name)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(root: String) : AbstractCommand("$root.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm")
        }

        suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1) ?: return
            val guild = context.guild
            val member = guild.retrieveMember(user).awaitOrNull()

            context.daoManager.forceRoleWrapper.remove(context.guildId, user.idLong, role.idLong)
            if (member != null && member.roles.contains(role)) {
                if (!guild.selfMember.canInteract(role) || !guild.removeRoleFromMember(member, role)
                        .reason("(forceRole remove) ${context.author.asTag}").awaitBool()
                ) {
                    LogUtils.sendMessageFailedToRemoveRoleFromMember(context.daoManager, member, role)
                    return
                }
            }

            val msg = context.getTranslation("$root.success")
                .withSafeVariable(PLACEHOLDER_USER, user.asTag)
                .withSafeVariable(PLACEHOLDER_ROLE, role.name)
            sendRsp(context, msg)
        }
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        suspend fun execute(context: ICommandContext) {
            val userRolesMap = context.daoManager.forceRoleWrapper.getForceRoles(context.guildId)
            val reverseMap = mutableMapOf<Long, Int>()

            for ((_, roleIds) in userRolesMap) {
                for (roleId in roleIds) {
                    reverseMap[roleId] = (reverseMap[roleId] ?: 0) + 1
                }
            }

            if (reverseMap.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendRsp(context, msg)
                return
            }


            var content = "```INI\n[role] - [users]"
            for ((roleId, users) in reverseMap) {
                val role = context.guild.getRoleById(roleId)
                content += "\n[${role?.name}] - $users"
            }
            content += "```"


            val msg = context.getTranslation("$root.title") + content
            sendRspCodeBlock(context, msg, "INI", true)
        }
    }
}