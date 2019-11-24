package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

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
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1, sameGuildAsContext = true, canInteract = true) ?: return
            val member = context.guild.getMember(user)

            context.daoManager.forceRoleWrapper.add(context.guildId, user.idLong, role.idLong)
            if (member != null && !member.roles.contains(role)) {
                context.guild.addRoleToMember(member, role).queue()
            }
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
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
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1) ?: return
            val member = context.guild.getMember(user)

            context.daoManager.forceRoleWrapper.remove(context.guildId, user.idLong, role.idLong)
            if (member != null && member.roles.contains(role)) {
                context.guild.removeRoleFromMember(member, role).queue()
            }

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
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

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.title")
                .replace(PLACEHOLDER_USER, user.asTag)
            sendMsg(context, msg)
        }
    }
}