package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.internals.TriState
import me.melijn.melijnbot.objects.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.toLC


const val MESSAGE_UNKNOWN_TRISTATE = "message.unknown.tristate"

class SetPrivateAllowSpacedPrefixState : AbstractCommand("command.setprivateallowspacedprefixstate") {

    init {
        id = 176
        name = "setPrivateAllowSpacedPrefixState"
        aliases = arrayOf("spasps")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.allowSpacedPrefixWrapper
        if (context.args.isEmpty()) {
            val allowed = wrapper.privateAllowSpacedPrefixGuildCache.get(context.authorId).await()
            val msg = context.getTranslation("$root.show.${allowed.toLC()}")
            sendMsg(context, msg)
            return
        }

        val state = getEnumFromArgNMessage<TriState>(context, 0, MESSAGE_UNKNOWN_TRISTATE) ?: return
        wrapper.setUserState(context.guildId, state)
        val msg = context.getTranslation("$root.set.${state.toLC()}")
        sendMsg(context, msg)
    }
}