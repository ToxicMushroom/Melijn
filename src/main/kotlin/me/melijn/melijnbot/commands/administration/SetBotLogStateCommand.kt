package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp

class SetBotLogStateCommand : AbstractCommand("command.setbotlogstate") {

    init {
        id = 166
        name = "setBotLogState"
        aliases = arrayOf("sbls")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.botLogStateWrapper
        if (context.args.isEmpty()) {
            val state = wrapper.botLogStateCache.get(context.guildId).await()
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