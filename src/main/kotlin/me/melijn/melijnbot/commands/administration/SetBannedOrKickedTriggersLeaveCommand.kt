package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class SetBannedOrKickedTriggersLeaveCommand : AbstractCommand("command.setbannedorkickedtriggersleave") {

    init {
        id = 164
        name = "setBannedOrKickedTriggersLeave"
        aliases = arrayOf("sboktl")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.bannedOrKickedTriggersLeaveWrapper
        if (context.args.isEmpty()) {
            val state = wrapper.bannedOrKickedTriggersLeaveCache.get(context.guildId).await()
            val msg = context.getTranslation("$root.show.$state")
            sendRsp(context, msg)
        }

        val newState = getBooleanFromArgNMessage(context, 0) ?: return
        wrapper.set(context.guildId, newState)

        val msg = context.getTranslation("$root.set.$newState")
        sendRsp(context, msg)
    }
}