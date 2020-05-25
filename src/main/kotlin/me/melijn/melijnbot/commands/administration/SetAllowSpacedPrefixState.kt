package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

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
            sendMsg(context, msg)
            return
        }

        val state = getBooleanFromArgNMessage(context, 0) ?: return
        wrapper.setGuildState(context.guildId, state)
        val msg = context.getTranslation("$root.set.$state")
        sendMsg(context, msg)
    }
}