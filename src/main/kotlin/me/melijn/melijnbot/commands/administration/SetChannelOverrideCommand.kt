package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commands.utility.MESSAGE_UNKNOWN_TRISTATE
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.TriState
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission

class SetChannelOverrideCommand : AbstractCommand("command.setchanneloverride") {

    init {
        name = "setChannelOverride"
        aliases = arrayOf("sco")
        discordChannelPermissions = arrayOf(Permission.MANAGE_PERMISSIONS, Permission.MANAGE_CHANNEL)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        val role = getRoleByArgsNMessage(context, 0, true, true) ?: return
        val channel = getChannelByArgsNMessage(context, 1, true) ?: return
        var triState1 = TriState.TRUE
        var triState2 = TriState.DEFAULT

        if (context.args.size >  3) {
            triState1 = getEnumFromArgNMessage(context, 2, MESSAGE_UNKNOWN_TRISTATE) ?: return
            triState2 = getEnumFromArgNMessage(context, 3, MESSAGE_UNKNOWN_TRISTATE) ?: return
        }

        val mapper: (Boolean) -> TriState = {
            if (it) triState1
            else triState2
        }

        var action = channel.upsertPermissionOverride(role)
        for (perm in Permission.values()) {
            val hasPerm = role.permissions.contains(perm)
            action = when (mapper(hasPerm)) {
                TriState.TRUE -> action.grant(perm.rawValue)
                TriState.DEFAULT -> action.clear(perm.rawValue)
                TriState.FALSE -> action.deny(perm.rawValue)
            }
        }
        action.reason("(${context.author.asTag}): setChannelOverride").await()

        sendRsp(context, "Successfully created the override on ${channel.asTag} for **@${role.name}** with mappings `on -> $triState1` and `off -> $triState2`")
    }
}