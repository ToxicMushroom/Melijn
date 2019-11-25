package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_ROLETYPE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetRoleCommand : AbstractCommand("command.setrole") {

    init {
        id = 29
        name = "setRole"
        aliases = arrayOf("setr")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }


        val roleType: RoleType = getEnumFromArgNMessage(context, 0, MESSAGE_UNKNOWN_ROLETYPE) ?: return

        handleEnum(context, roleType)
    }

    private suspend fun handleEnum(context: CommandContext, roleType: RoleType) {
        if (context.args.size > 1) {
            setRole(context, roleType)
        } else {
            displayRole(context, roleType)
        }
    }

    private suspend fun displayRole(context: CommandContext, roleType: RoleType) {
        val daoWrapper = context.daoManager.roleWrapper
        val pair = Pair(context.guildId, roleType)
        val roleId = daoWrapper.roleCache.get(pair).await()
        val role = context.guild.getRoleById(roleId)

        if (roleId != -1L && role == null) {
            daoWrapper.removeRole(pair.first, pair.second)
            return
        }

        val language = context.getLanguage()
        val msg = (if (role != null) {
            i18n.getTranslation(language, "$root.show.set")
                .replace(PLACEHOLDER_ROLE, role.name)
        } else {
            i18n.getTranslation(language, "$root.show.unset")
        }).replace("%roleType%", roleType.text)

        sendMsg(context, msg)
    }


    private suspend fun setRole(context: CommandContext, roleType: RoleType) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val language = context.getLanguage()
        val daoWrapper = context.daoManager.roleWrapper
        val msg = if (context.args[1].equals("null", true)) {

            daoWrapper.removeRole(context.guildId, roleType)

            i18n.getTranslation(language, "$root.unset")
                .replace("%roleType%", roleType.text)
        } else {
            val role = getRoleByArgsNMessage(context, 1) ?: return
            daoWrapper.setRole(context.guildId, roleType, role.idLong)

            i18n.getTranslation(language, "$root.set")
                .replace("%roleType%", roleType.text)
                .replace(PLACEHOLDER_ROLE, role.name)

        }
        sendMsg(context, msg)
    }
}