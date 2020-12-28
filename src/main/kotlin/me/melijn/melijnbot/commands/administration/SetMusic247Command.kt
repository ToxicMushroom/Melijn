package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class SetMusic247Command : AbstractCommand("command.setmusic247") {

    init {
        id = 146
        name = "setMusic247Mode"
        aliases = arrayOf("sm247m", "setRadioMode", "srm")
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val wrapper = context.daoManager.music247Wrapper
        if (context.args.isEmpty()) {
            val enabled = wrapper.is247Mode(context.guildId)
            val extra = if (enabled) "enabled" else "disabled"
            val msg = context.getTranslation("$root.show.$extra")

            sendRsp(context, msg)
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
        sendRsp(context, msg)
    }
}