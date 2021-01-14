package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp

class RepCommand : AbstractCommand("command.rep") {

    init {
        id = 237
        name = "rep"
        runConditions = arrayOf(RunCondition.GUILD)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            val rep = context.daoManager.repWrapper.getRep(context.authorId)
            val dailyCooldownWrapper = context.daoManager.economyCooldownWrapper
            val lastTime = dailyCooldownWrapper.getCooldown(context.authorId, name)
            val difference = System.currentTimeMillis() - lastTime
            val extra = if (difference < 86400000) {
                ". You can rep again in: `" + getDurationString(86400000 - difference) + "`"
            } else ""

            val msg = context.getTranslation("$root.showrep")
                .withVariable("rep", rep) + extra
            sendRsp(context, msg)
        } else {
            if (!canRepElseMessage(context)) return
            val user = retrieveMemberByArgsNMessage(context, 0, false, botAllowed = false) ?: return
            if (user.idLong == context.authorId) {
                val msg = context.getTranslation("$root.selfrep")
                sendRsp(context, msg)
                return
            }
            context.daoManager.economyCooldownWrapper.setCooldown(context.authorId, name, System.currentTimeMillis())
            val rep = context.daoManager.repWrapper.increment(user.idLong)
            val msg = context.getTranslation("$root.gaverep")
                .withSafeVariable(PLACEHOLDER_USER, user.asTag)
                .withVariable("rep", rep)
            sendRsp(context, msg)
        }
    }

    private suspend fun canRepElseMessage(context: ICommandContext): Boolean {
        val dailyCooldownWrapper = context.daoManager.economyCooldownWrapper
        val lastTime = dailyCooldownWrapper.getCooldown(context.authorId, name)
        val difference = System.currentTimeMillis() - lastTime
        if (difference > 86400000) {
            return true
        }

        val msg = context.getTranslation("$root.oncooldown")
            .withVariable("duration", getDurationString(86400000 - difference))
        sendRsp(context, msg)
        return false
    }
}