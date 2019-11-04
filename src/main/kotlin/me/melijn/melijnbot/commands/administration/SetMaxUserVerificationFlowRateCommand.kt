package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getLongFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SetMaxUserVerificationFlowRateCommand : AbstractCommand("command.setmaxuserverificationflowrate") {

    init {
        id = 41
        name = "setMaxUserVerificationFlowRate"
        aliases = arrayOf("smuvfr")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationUserFlowRateWrapper
        val language = context.getLanguage()
        if (context.args.isEmpty()) {
            val flowRate = wrapper.verificationUserFlowRateCache.get(context.guildId).await()
            val part = if (flowRate == -1L) "unset" else "set"
            val msg = i18n.getTranslation(language, "$root.show.$part")
                .replace("%rate%", flowRate.toString())
            sendMsg(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.removeFlowRate(context.guildId)
            i18n.getTranslation(language, "$root.unset")
        } else {
            val rate = getLongFromArgNMessage(context, 0, 0) ?: return
            wrapper.setUserFlowRate(context.guildId, rate)
            i18n.getTranslation(language, "$root.set")
                .replace(PLACEHOLDER_ARG, context.rawArg)
        }

        sendMsg(context, msg)
    }
}