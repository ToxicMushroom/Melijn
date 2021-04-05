package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role

class ToggleRoleCommand : AbstractCommand("command.togglerole") {

    init {
        name = "toggleRole"
        aliases = arrayOf("tr")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }
        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val role = (getRoleByArgsNMessage(context, 1, true, canInteract = true)) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull() ?: return

        val cantIntereact = !context.member.canInteract(role)
        val isMissingPerms = !hasPermission(context, SpecialPermission.TOGGLEROLE_BYPASS_HIGHER.node)
        if (cantIntereact && isMissingPerms) {
            val msg = context.getTranslation("$root.higher.and.nopermission")
                .withVariable("permission", SpecialPermission.TOGGLEROLE_BYPASS_HIGHER.node)
                .withVariable("role", role.asMention)
            sendRsp(context, msg)
            return
        }
        if (member.roles.any { memberRole: Role ->
                memberRole.idLong == role.idLong
            }
        ) {
            val msg = context.getTranslation("$root.took")
                .withVariable("role", role.asMention)
                .withSafeVariable("user", member.asTag)

            context.guild.removeRoleFromMember(member, role).reason("(toggleRole) ${context.author.asTag}")
                .awaitOrNull()
            sendRsp(context, msg)
        } else {
            context.guild.addRoleToMember(member, role).reason("(toggleRole) ${context.author.asTag}").awaitOrNull()
            val msg = context.getTranslation("$root.gave")
                .withSafeVariable("user", member.asTag)
                .withVariable("role", role.asMention)
            sendRsp(context, msg)

        }
    }

}