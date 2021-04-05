package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class SetMaxUserVerificationFlowRateCommand : AbstractCommand("command.setmaxuserverificationflowrate") {

    init {
        id = 41
        name = "setMaxUserVerificationFlowRate"
        aliases = arrayOf("smuvfr")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    suspend fun execute(context: ICommandContext) {
        val wrapper = context.daoManager.verificationUserFlowRateWrapper
        if (context.args.isEmpty()) {
            val flowRate = wrapper.getFlowRate(context.guildId)
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