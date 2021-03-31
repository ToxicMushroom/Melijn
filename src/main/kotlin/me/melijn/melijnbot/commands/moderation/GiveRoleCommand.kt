package me.melijn.melijnbot.commands.moderation


import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission

class GiveRoleCommand : AbstractCommand("command.giverole") {
    init {
        name = "giveRole"
        aliases = arrayOf("give", "giveRole")
        commandCategory = CommandCategory.MODERATION
        discordChannelPermissions = arrayOf(Permission.MANAGE_ROLES)
    }


    override suspend fun execute(context: ICommandContext) {

        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return
        val role = (getRoleByArgsNMessage(context, 1, true, true)) ?: return
        val member = context.guild.retrieveMember(targetUser).awaitOrNull() ?: return

        if (!context.guild.selfMember.canInteract(member)) {
            val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
                .withVariable(PLACEHOLDER_USER, member.asTag)
            sendRsp(context, msg)
            return
        }

        context.guild.addRoleToMember(member, role).reason("(giveRole) ${context.author.asTag}").awaitOrNull()
        sendRsp(context, "Gave ${member.asTag} the ${role.name} role.")
    }
}




