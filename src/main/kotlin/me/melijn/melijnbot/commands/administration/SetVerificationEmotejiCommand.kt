package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getEmoteOrEmojiByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SetVerificationEmotejiCommand : AbstractCommand("command.setverificationemoteji") {

    init {
        id = 44
        name = "setVerificationEmoteji"
        aliases = arrayOf("sve")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationEmotejiWrapper
        val language = context.getLanguage()
        if (context.args.isEmpty()) {
            val code = wrapper.verificationEmotejiCache.get(context.getGuildId()).await()
            val part = if (code.isBlank()) "unset" else "set"
            val msg = i18n.getTranslation(language, "$root.show.$part")
                .replace("%code%", code)
            sendMsg(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.removeEmoteji(context.getGuildId())
            i18n.getTranslation(language, "$root.unset")
        } else {
            val emoteji = getEmoteOrEmojiByArgsNMessage(context, 0, false) ?: return
            val second = emoteji.second
            val first = emoteji.first
            when {
                second != null -> {
                    wrapper.setEmoteji(context.getGuildId(), second)
                    i18n.getTranslation(language, "$root.set.emoji")
                        .replace(PLACEHOLDER_ARG, second)

                }
                first != null -> {
                    wrapper.setEmoteji(context.getGuildId(), first.id)
                    i18n.getTranslation(language, "$root.set.emote")
                        .replace("%emoteId%", first.id)
                        .replace("%emoteName%", first.name)
                }
                else -> throw IllegalArgumentException("This should never be executed")
            }
        }

        sendMsg(context, msg)
    }
}