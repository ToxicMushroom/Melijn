package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.getColorFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.toHexString
import me.melijn.melijnbot.internals.utils.withVariable

class SetEmbedColorCommand : AbstractCommand("command.setembedcolor") {

    init {
        id = 77
        name = "setEmbedColor"
        aliases = arrayOf("sec")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.embedColorWrapper
        val msg = if (context.args.isEmpty()) {
            val colorInt = wrapper.getColor(context.guildId)

            if (colorInt == 0) {
                context.getTranslation("$root.show.unset")
            } else {
                context.getTranslation("$root.show.set")
                    .withVariable("color", colorInt.toHexString())
            }
        } else {
            if (context.rawArg.equals("null", true)) {
                wrapper.removeColor(context.guildId)

                context.getTranslation("$root.unset")
            } else {
                val color = getColorFromArgNMessage(context, 0) ?: return
                wrapper.setColor(context.guildId, color.rgb)

                context.getTranslation("$root.set")
                    .withVariable(PLACEHOLDER_ARG, color.rgb.toHexString())
            }
        }
        sendRsp(context, msg)
    }
}