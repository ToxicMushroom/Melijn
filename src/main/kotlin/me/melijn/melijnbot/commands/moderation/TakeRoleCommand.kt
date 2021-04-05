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


class TakeRoleCommand : AbstractCommand("command.takerole") {

    init {
        name = "takeRole"
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

        if(!context.member.canInteract(role) && !hasPermission(context, SpecialPermission.TAKEROLE_BYPASS_HIGHER.node)) {
            val msg = context.getTranslation("$root.higher.and.nopermission")
                .withVariable("permission", SpecialPermission.TAKEROLE_BYPASS_HIGHER.node)
                .withVariable("role", role.asMention)
            sendRsp(context, msg)
            return
        }

        if (member.roles.none { memberRole: Role ->
                memberRole.idLong == role.idLong
            }
        ) {
            val msg = context.getTranslation("$root.missingrole")
                .withSafeVariable("user", member.asTag)
            sendRsp(context, msg)
            return
        }

        val msg = context.getTranslation("$root.took")
            .withVariable("role", role.asMention)
            .withSafeVariable("user", member.asTag)

        context.guild.removeRoleFromMember(member, role).reason("(removeRole) ${context.author.asTag}").awaitOrNull()
        sendRsp(context, msg)
    }
}