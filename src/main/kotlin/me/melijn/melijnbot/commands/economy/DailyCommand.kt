package me.melijn.melijnbot.commands.economy

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class DailyCommand : AbstractCommand("command.daily") {

    init {
        id = 193
        name = "daily"
        commandCategory = CommandCategory.ECONOMY
    }

    override suspend fun execute(context: CommandContext) {
        if (canDailyElseMessage(context)) {
            val balanceWrapper = context.daoManager.balanceWrapper
            val cash = balanceWrapper.balanceCache.get(context.authorId).await()
            balanceWrapper.setBalance(context.authorId, cash + 100)
            context.daoManager.dailyCooldownWrapper.setCooldown(context.authorId, System.currentTimeMillis())

            val msg = context.getTranslation("$root.got")
                .withVariable("daily", 100)
                .withVariable("cash", cash+100)
            sendRsp(context, msg)
        }
    }

    private suspend fun canDailyElseMessage(context: CommandContext): Boolean {
        val dailyCooldownWrapper = context.daoManager.dailyCooldownWrapper
        val lastTime = dailyCooldownWrapper.cooldownCache.get(context.authorId).await()
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