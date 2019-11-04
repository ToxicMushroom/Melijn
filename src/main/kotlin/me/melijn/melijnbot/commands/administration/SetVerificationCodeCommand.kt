package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg

class SetVerificationCodeCommand : AbstractCommand("command.setverificationcode") {

    init {
        id = 42
        name = "setVerificationCode"
        aliases = arrayOf("svc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationCodeWrapper
        val language = context.getLanguage()
        if (context.args.isEmpty()) {
            val code = wrapper.verificationCodeCache.get(context.guildId).await()
            val part = if (code.isBlank()) "unset" else "set"
            val msg = i18n.getTranslation(language, "$root.show.$part")
                .replace("%code%", code)
            sendMsg(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.removeCode(context.guildId)
            i18n.getTranslation(language, "$root.unset")
        } else {
            wrapper.setCode(context.guildId, context.rawArg)
            i18n.getTranslation(language, "$root.set")
                .replace(PLACEHOLDER_ARG, context.rawArg)
        }

        sendMsg(context, msg)
    }
}