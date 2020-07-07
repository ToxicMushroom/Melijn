package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.getLongFromArgNMessage
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.withVariable

class SetMaxUserVerificationFlowRateCommand : AbstractCommand("command.setmaxuserverificationflowrate") {

    init {
        id = 41
        name = "setMaxUserVerificationFlowRate"
        aliases = arrayOf("smuvfr")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.verificationUserFlowRateWrapper
        if (context.args.isEmpty()) {
            val flowRate = wrapper.verificationUserFlowRateCache.get(context.guildId).await()
            val part = if (flowRate == -1L) {
                "unset"
            } else {
                "set"
            }

            val msg = context.getTranslation("$root.show.$part")
                .withVariable("rate", flowRate.toString())
            sendRsp(context, msg)
            return
        }

        val msg = if (context.rawArg == "null") {
            wrapper.removeFlowRate(context.guildId)
            context.getTranslation("$root.unset")
        } else {
            val rate = getLongFromArgNMessage(context, 0, 0) ?: return
            wrapper.setUserFlowRate(context.guildId, rate)
            context.getTranslation("$root.set")
                .withVariable(PLACEHOLDER_ARG, context.rawArg)
        }

        sendRsp(context, msg)
    }
}