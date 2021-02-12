package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class SetBannedOrKickedTriggersLeaveCommand : AbstractCommand("command.setbannedorkickedtriggersleave") {

    init {
        id = 164
        name = "setBannedOrKickedTriggersLeave"
        aliases = arrayOf("sboktl")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val wrapper = context.daoManager.bannedOrKickedTriggersLeaveWrapper
        if (context.args.isEmpty()) {
            val state = wrapper.shouldTrigger(context.guildId)
            val msg = context.getTranslation("$root.show.$state")
            sendRsp(context, msg)
            return
        }

        val newState = getBooleanFromArgNMessage(context, 0) ?: return
        wrapper.set(context.guildId, newState)

        val msg = context.getTranslation("$root.set.$newState")
        sendRsp(context, msg)
    }
}