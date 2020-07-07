package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_ROLETYPE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.objects.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.message.sendSyntax
import me.melijn.melijnbot.objects.utils.toUCC
import me.melijn.melijnbot.objects.utils.withVariable

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
        val daoManager = context.daoManager
        val role = context.guild.getAndVerifyRoleByType(daoManager, roleType)

        val msg = (if (role != null) {
            context.getTranslation("$root.show.set")
                .withVariable(PLACEHOLDER_ROLE, role.name)
        } else {
            context.getTranslation("$root.show.unset")
        }).withVariable("roleType", roleType.toUCC())

        sendRsp(context, msg)
    }


    private suspend fun setRole(context: CommandContext, roleType: RoleType) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val daoWrapper = context.daoManager.roleWrapper
        val msg = if (context.args[1].equals("null", true)) {

            daoWrapper.removeRole(context.guildId, roleType)

            context.getTranslation("$root.unset")
                .withVariable("roleType", roleType.toUCC())
        } else {
            val role = getRoleByArgsNMessage(context, 1) ?: return
            daoWrapper.setRole(context.guildId, roleType, role.idLong)

            context.getTranslation("$root.set")
                .withVariable("roleType", roleType.toUCC())
                .withVariable(PLACEHOLDER_ROLE, role.name)

        }
        sendRsp(context, msg)
    }
}