package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getDurationByArgsNMessage
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable

class SetAutoRemoveInactiveJoinMessagesDuration : AbstractCommand("command.setautoremoveinactivejoinmessagesduration") {

    init {
        name = "setAutoremoveInactiveJoinMessagesDuration"
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        aliases = arrayOf("sarmijmd")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val wrapper = context.daoManager.autoRemoveInactiveJoinMessageWrapper
        if (context.args.isEmpty()) {
            val current = wrapper.get(context.guildId)
            if (current == -1L) {
                sendRsp(
                    context,
                    "Join/LeaveMessages from members who never sent anything are currenly not being removed when they leave."
                )
            } else {
                sendRsp(
                    context,
                    "Join/LeaveMessages from members who never sent anything are currenly being removed if they left within **%duration%**."
                        .withSafeVariable("duration", getDurationString(current))
                )
            }
            return
        }

        if (context.args[0] == "null") {
            wrapper.delete(context.guildId)
            sendRsp(context, "Not deleting join and leave message anymore.")
            return
        }

        val duration = getDurationByArgsNMessage(context, 0, context.args.size) ?: return
        if (duration < 1 || duration > 3600 * 48) {
            sendRsp(context, "Duration has to lie between 1 second and 48 hours")
            return
        }
        wrapper.set(context.guildId, duration)
        sendRsp(
            context,
            "Set Join/LeaveMessages from inactive members to be removed if they left within **%duration%** of joining."
                .withVariable("duration", getDurationString(duration * 1000))
        )
    }
}