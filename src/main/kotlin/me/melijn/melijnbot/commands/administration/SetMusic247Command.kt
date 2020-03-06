package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SetMusic247Command : AbstractCommand("command.setmusic247") {

    init {
        id = 146
        name = "setMusic247"
        aliases = arrayOf("sm247", "setRadioMode", "srm")
        runConditions = arrayOf(RunCondition.SUPPORTER)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.music247Wrapper
        if (context.args.isEmpty()) {
            val enabled = wrapper.music247Cache.get(context.guildId).await()
            val extra = if (enabled) "enabled" else "disabled"
            val msg = context.getTranslation("$root.show.$extra")

            sendMsg(context, msg)
            return
        }

        val newState = getBooleanFromArgNMessage(context, 0) ?: return

        if (newState) {
            wrapper.add(context.guildId)
        } else {
            wrapper.remove(context.guildId)
        }

        val extra = if (newState) "enabled" else "disabled"
        val msg = context.getTranslation("$root.set.$extra")
        sendMsg(context, msg)
    }
}