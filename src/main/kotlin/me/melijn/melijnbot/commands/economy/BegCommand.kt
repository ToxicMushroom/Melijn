package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable
import kotlin.random.Random

class BegCommand : AbstractCommand("command.beg") {

    init {
        id = 238
        name = "beg"
        commandCategory = CommandCategory.ECONOMY
    }

    override suspend fun execute(context: CommandContext) {
        if (!canBegElseMessage(context)) return

        val balanceWrapper = context.daoManager.balanceWrapper
        val reward = Random.nextInt(10)
        val cash = balanceWrapper.getBalance(context.authorId)

        balanceWrapper.setBalance(context.authorId, cash + reward)
        context.daoManager.economyCooldownWrapper.setCooldown(context.authorId, name, System.currentTimeMillis())

        val msg = context.getTranslation("$root.got")
            .withVariable("beg", reward)
            .withVariable("cash", cash + reward)
        sendRsp(context, msg)
    }

    private suspend fun canBegElseMessage(context: CommandContext): Boolean {
        val dailyCooldownWrapper = context.daoManager.economyCooldownWrapper
        val lastTime = dailyCooldownWrapper.getCooldown(context.authorId, name)
        val difference = System.currentTimeMillis() - lastTime
        if (difference > 3600000) {
            return true
        }

        val msg = context.getTranslation("$root.oncooldown")
            .withVariable("duration", getDurationString(3600000 - difference))
        sendRsp(context, msg)
        return false
    }
}