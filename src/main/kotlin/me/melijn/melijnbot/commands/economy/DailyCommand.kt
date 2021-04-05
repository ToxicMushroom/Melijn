package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.isPremiumUser
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class DailyCommand : AbstractCommand("command.daily") {

    init {
        id = 193
        name = "daily"
        commandCategory = CommandCategory.ECONOMY
    }

    suspend fun execute(context: ICommandContext) {
        if (canDailyElseMessage(context)) {
            val balanceWrapper = context.daoManager.balanceWrapper
            val cash = balanceWrapper.getBalance(context.authorId)
            val premium = isPremiumUser(context, context.author)
            val reward = if (premium) 200 else 100
            balanceWrapper.setBalance(context.authorId, cash + reward)
            context.daoManager.economyCooldownWrapper.setCooldown(context.authorId, name, System.currentTimeMillis())

            val msg = context.getTranslation("$root.got")
                .withVariable("daily", reward)
                .withVariable("cash", cash + reward)
            sendRsp(context, msg)
        }
    }

    private suspend fun canDailyElseMessage(context: ICommandContext): Boolean {
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