package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class SetAllowSpacedPrefixState : AbstractCommand("command.setallowspacedprefixstate") {

    init {
        id = 174
        name = "setAllowSpacedPrefixState"
        aliases = arrayOf("sasps")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.allowSpacedPrefixWrapper
        if (context.args.isEmpty()) {
            val allowed = wrapper.allowSpacedPrefixGuildCache.get(context.guildId).await()
            val msg = context.getTranslation("$root.show.$allowed")
            sendRsp(context, msg)
            return
        }

        val state = getBooleanFromArgNMessage(context, 0) ?: return
        wrapper.setGuildState(context.guildId, state)
        val msg = context.getTranslation("$root.set.$state")
        sendRsp(context, msg)
    }
}