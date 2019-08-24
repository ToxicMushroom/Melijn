package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.enumValueOrNull
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetRoleCommand : AbstractCommand("command.setrole") {

    init {
        id = 29
        name = "setRole"
        aliases = arrayOf("sr")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
        }


        val roleType: RoleType? = enumValueOrNull(context.args[0])
        if (roleType == null) {
            sendSyntax(context, syntax)
            return
        }

        handleEnum(context, roleType)
    }

    private fun handleEnum(context: CommandContext, roleType: RoleType) {
        if (context.args.size > 1) {
            setRole(context, roleType)
        } else {
            displayRole(context, roleType)
        }
    }

    private fun displayRole(context: CommandContext, roleType: RoleType) {
        val daoWrapper = context.daoManager.roleWrapper
        val pair = Pair(context.getGuildId(), roleType)
        val roleId = daoWrapper.roleCache.get(pair).get()
        val role = context.getGuild().getRoleById(roleId)

        if (roleId != -1L && role == null) {
            daoWrapper.removeRole(pair.first, pair.second)
            return
        }

        val msg = (if (role != null) {
            Translateable("$root.show.set").string(context)
                    .replace("%role%", role.name)
        } else {
            Translateable("$root.show.unset").string(context)
        }).replace("%roleType%", roleType.text)

        sendMsg(context, msg)
    }



    private fun setRole(context: CommandContext, roleType: RoleType) {
        if (context.args.size < 2) {
            sendSyntax(context, syntax)
            return
        }

        val daoWrapper = context.daoManager.roleWrapper
        val msg = if (context.args[1].equals("null", true)) {

            daoWrapper.removeRole(context.getGuildId(), roleType)

            Translateable("$root.unset").string(context)
                    .replace("%roleType%", roleType.text)
        } else {
            val role = getRoleByArgsNMessage(context, 1) ?: return
            daoWrapper.setRole(context.getGuildId(), roleType, role.idLong)

            Translateable("$root.set.single").string(context)
                    .replace("%roleType%", roleType.text)
                    .replace("%role%", role.name)

        }
        sendMsg(context, msg)
    }
}