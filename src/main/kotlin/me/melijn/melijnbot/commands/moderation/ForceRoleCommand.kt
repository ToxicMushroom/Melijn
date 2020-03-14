package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.utils.*

class ForceRoleCommand : AbstractCommand("command.forcerole") {

    init {
        id = 38
        name = "forceRole"
        aliases = arrayOf("fr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root)
        )
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class AddArg(root: String) : AbstractCommand("$root.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1, sameGuildAsContext = true, canInteract = true) ?: return
            val member = context.guild.retrieveMember(user).await()

            context.daoManager.forceRoleWrapper.add(context.guildId, user.idLong, role.idLong)
            if (member != null && !member.roles.contains(role)) {
                if (!context.guild.addRoleToMember(member, role).reason("forcerole").awaitBool()) {
                    LogUtils.sendMessageFailedToAddRoleToMember(context.daoManager, member, role)
                }
            }

            val msg = context.getTranslation("$root.success")
                .replace(PLACEHOLDER_USER, user.asTag)
                .replace(PLACEHOLDER_ROLE, role.name)
            sendMsg(context, msg)
        }
    }

    class RemoveArg(root: String) : AbstractCommand("$root.remove") {

        init {
            name = "remove"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1) ?: return
            val member = context.guild.retrieveMember(user).await()

            context.daoManager.forceRoleWrapper.remove(context.guildId, user.idLong, role.idLong)
            if (member != null && member.roles.contains(role)) {
                if (context.guild.removeRoleFromMember(member, role).reason("deforceroled").awaitBool()) {
                    LogUtils.sendMessageFailedToRemoveRoleFromMember(context.daoManager, member, role)
                }
            }

            val msg = context.getTranslation("$root.success")
                .replace(PLACEHOLDER_USER, user.asTag)
                .replace(PLACEHOLDER_ROLE, role.name)
            sendMsg(context, msg)
        }
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val user = retrieveUserByArgsNMessage(context, 0) ?: return

            val roleIds = context.daoManager.forceRoleWrapper.forceRoleCache.get(context.guildId).await()
                .getOrDefault(user.idLong, emptyList())

            var content = "```INI"
            for (roleId in roleIds) {
                val role = context.guild.getRoleById(roleId)
                content += "\n[${role?.name} - $roleId"
            }
            if (roleIds.isEmpty()) content += "/"
            content += "```"

            val msg = context.getTranslation("$root.title")
                .replace(PLACEHOLDER_USER, user.asTag)
            sendMsg(context, msg)
        }
    }
}