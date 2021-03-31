package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext

import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_ROLE_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ROLE

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
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
    }


    override suspend fun execute(context: ICommandContext) {

        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val role = (getRoleByArgsNMessage(context, 1)) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull() ?: return

        if (!context.selfMember.canInteract(role)) {
            val msg = context.getTranslation(MESSAGE_SELFINTERACT_ROLE_HIARCHYEXCEPTION)
                .withVariable(PLACEHOLDER_ROLE, role.name)
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