package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.TriState
import me.melijn.melijnbot.internals.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.toLC


const val MESSAGE_UNKNOWN_TRISTATE = "message.unknown.tristate"

class SetPrivateAllowSpacedPrefixState : AbstractCommand("command.setprivateallowspacedprefixstate") {

    init {
        id = 176
        name = "setPrivateAllowSpacedPrefixState"
        aliases = arrayOf("spasps")
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(context: ICommandContext) {
        val wrapper = context.daoManager.allowSpacedPrefixWrapper
        if (context.args.isEmpty()) {
            val allowed = wrapper.getUserTriState(context.authorId)
            val msg = context.getTranslation("$root.show.${allowed.toLC()}")
            sendRsp(context, msg)
            return
        }

        val state = getEnumFromArgNMessage<TriState>(context, 0, MESSAGE_UNKNOWN_TRISTATE) ?: return
        wrapper.setUserState(context.authorId, state)

        val msg = context.getTranslation("$root.set.${state.toLC()}")
        sendRsp(context, msg)
    }
}