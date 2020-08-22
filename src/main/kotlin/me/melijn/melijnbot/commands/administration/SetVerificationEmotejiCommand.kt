package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.getEmotejiByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class SetVerificationEmotejiCommand : AbstractCommand("command.setverificationemoteji") {

    init {
        id = 44
        name = "setVerificationEmoteji"
        aliases = arrayOf("sve", "setVerificationEmoji", "setVerificationEmote")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationEmotejiWrapper
        if (context.args.isEmpty()) {
            val code = wrapper.getEmoteji(context.guildId)
            val part = if (code.isBlank()) {
                "unset"
            } else {
                "set"
            }
            val msg = context.getTranslation("$root.show.$part")
                .withVariable("code", code)
            sendRsp(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.removeEmoteji(context.guildId)
            context.getTranslation("$root.unset")
        } else {
            val emoteji = getEmotejiByArgsNMessage(context, 0) ?: return
            val second = emoteji.second
            val first = emoteji.first
            when {
                second != null -> {
                    wrapper.setEmoteji(context.guildId, second)
                    context.getTranslation("$root.set.emoji")
                        .withVariable(PLACEHOLDER_ARG, second)

                }
                first != null -> {
                    wrapper.setEmoteji(context.guildId, first.id)
                    context.getTranslation("$root.set.emote")
                        .withVariable("emoteId", first.id)
                        .withVariable("emoteName", first.name)
                }
                else -> throw IllegalArgumentException("This should never be executed")
            }
        }

        sendRsp(context, msg)
    }
}