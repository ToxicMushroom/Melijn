package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.VerificationType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.enumValueOrNull
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.toUCC

class SetVerificationTypeCommand : AbstractCommand("command.type") {

    init {
        id = 43
        name = "setVerificationType"
        aliases = arrayOf("svt")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationTypeWrapper
        val language = context.getLanguage()
        if (context.args.isEmpty()) {
            val type = wrapper.verificationTypeCache.get(context.getGuildId()).await()
            val part = if (type == VerificationType.NONE) "unset" else "set"
            val msg = i18n.getTranslation(language, "$root.show.$part")
                .replace("%type%", type.toUCC())
            sendMsg(context, msg)
            return
        }


        val type = enumValueOrNull<VerificationType>(context.rawArg)
        val msg = if (context.rawArg == "null" || type == VerificationType.NONE) {
            wrapper.removeCode(context.getGuildId())
            i18n.getTranslation(language, "$root.unset")
        } else if (type == null) {
            i18n.getTranslation(language, "message.unknown.verificationtype")
        } else {
            wrapper.setCode(context.getGuildId(), type)
            i18n.getTranslation(language, "$root.set")
                .replace(PLACEHOLDER_ARG, type.toUCC())
        }

        sendMsg(context, msg)
    }
}