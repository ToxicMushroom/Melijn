package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.argSizeCheckFailed
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission


class CloneRolePermissionsCommand : AbstractCommand("command.clonerolepermissions") {

    init {
        name = "cloneRolePermissions"
        aliases = arrayOf("crp")
        discordPermissions = arrayOf(Permission.MANAGE_ROLES, Permission.MANAGE_PERMISSIONS)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        val role1 = getRoleByArgsNMessage(context, 0, true, true) ?: return
        val role2 = getRoleByArgsNMessage(context, 1, true, true) ?: return

        var action = role2.manager

        var keepEnabled = false
        if (context.args.size > 2) {
            keepEnabled = getBooleanFromArgNMessage(context, 2) ?: return
        }

        action = if (keepEnabled) {
            action.setPermissions(role1.permissionsRaw or role2.permissionsRaw)
        } else {
            action.setPermissions(role1.permissionsRaw)
        }

        action.reason("(${context.author.asTag}): cloneRolePermissions").await()
        sendRsp(context, "Successfully copied **${role1.name}**'s discord permissions to **${role2.name}**")
    }
}