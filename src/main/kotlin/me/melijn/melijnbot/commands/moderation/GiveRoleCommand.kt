package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role

class GiveRoleCommand : AbstractCommand("command.giverole") {

    init {
        name = "giveRole"
        aliases = arrayOf("gr")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.MANAGE_ROLES)
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val role = (getRoleByArgsNMessage(context, 1, true, canInteract = true)) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull() ?: return
        if (cantGiveRoleAndRsp(context, role)) return

        if (member.roles.any { memberRole: Role ->
                memberRole.idLong == role.idLong
            }
        ) {
            val msg = context.getTranslation("$root.alreadyrole")
                .withSafeVariable("user", member.asTag)
            sendRsp(context, msg)
            return
        }

        context.guild.addRoleToMember(member, role).reason("(giveRole) ${context.author.asTag}").awaitOrNull()
        val msg = context.getTranslation("$root.gave")
            .withSafeVariable("user", member.asTag)
            .withVariable("role", role.asMention)
        sendRsp(context, msg)
    }

    private suspend fun cantGiveRoleAndRsp(
        context: ICommandContext,
        role: Role
    ): Boolean {
        val cantInteract = !context.member.canInteract(role)
        val isMissingPerms = !hasPermission(context, SpecialPermission.GIVEROLE_BYPASS_HIGHER.node)
        if (cantInteract && isMissingPerms) {
            val msg = context.getTranslation("$root.higher.and.nopermission")
                .withVariable("permission", SpecialPermission.GIVEROLE_BYPASS_HIGHER.node)
                .withVariable("role", role.asMention)
            sendRsp(context, msg)
            return true
        }
        return false
    }
}

