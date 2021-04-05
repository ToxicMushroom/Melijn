package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.getColorFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.toHexString
import me.melijn.melijnbot.internals.utils.withVariable

class SetPrivateEmbedColorCommand : AbstractCommand("command.setprivateembedcolor") {

    init {
        id = 76
        name = "setPrivateEmbedColor"
        aliases = arrayOf("spec")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(context: ICommandContext) {
        setEmbedColor(context) { it.authorId }
    }

    companion object {
        suspend fun setEmbedColor(context: ICommandContext, idParser: (ICommandContext) -> Long) {
            val root = context.commandOrder.first().root
            val wrapper = context.daoManager.embedColorWrapper
            val id = idParser(context)
            val msg = if (context.args.isEmpty()) {
                val colorInt = wrapper.getColor(id)

                if (colorInt == 0) {
                    context.getTranslation("$root.show.unset")
                } else {
                    context.getTranslation("$root.show.set")
                        .withVariable("color", colorInt.toHexString())
                }
            } else {
                if (context.rawArg.equals("null", true)) {
                    wrapper.removeColor(id)

                    context.getTranslation("$root.unset")
                } else {
                    val color = getColorFromArgNMessage(context, 0) ?: return
                    wrapper.setColor(id, color.rgb)

                    context.getTranslation("$root.set")
                        .withVariable(PLACEHOLDER_ARG, color.rgb.toHexString())
                }
            }
            sendRsp(context, msg)
        }
    }
}


