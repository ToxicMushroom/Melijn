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
        sendSyntax(context, syntax)
    }

    class AddArg(root: String) : AbstractCommand("$root.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1, true, true) ?: return
            val member = context.getGuild().getMember(user)

            context.daoManager.forceRoleWrapper.add(context.getGuildId(), user.idLong, role.idLong)
            if (member != null && !member.roles.contains(role)) {
                context.getGuild().addRoleToMember(member, role).queue()
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
            val member = context.getGuild().getMember(user)

            context.daoManager.forceRoleWrapper.remove(context.getGuildId(), user.idLong, role.idLong)
            if (member != null && member.roles.contains(role)) {
                context.getGuild().removeRoleFromMember(member, role).queue()
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

            val roleIds = context.daoManager.forceRoleWrapper.forceRoleCache.get(context.getGuildId()).await()
                .getOrDefault(user.idLong, emptyList())

            var content = "```INI"
            for (roleId in roleIds) {
                val role = context.getGuild().getRoleById(roleId)
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