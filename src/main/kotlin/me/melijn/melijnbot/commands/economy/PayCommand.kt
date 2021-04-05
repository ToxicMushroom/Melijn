package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withVariable

class PayCommand : AbstractCommand("command.pay") {

    init {
        id = 194
        name = "pay"
        aliases = arrayOf("give")
        commandCategory = CommandCategory.ECONOMY
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val balanceWrapper = context.daoManager.balanceWrapper
        val cash = balanceWrapper.getBalance(context.authorId)

        val amount = if (context.args[1].equals("all", true)) {
            cash

        } else {
            val amount = getLongFromArgNMessage(context, 1, 1) ?: return
            if (amount > cash) {
                val msg = context.getTranslation("$root.paytobig")
                    .withVariable("pay", amount)
                    .withVariable("cash", cash)
                    .withVariable("amount", amount)
                sendRsp(context, msg)
                return
            }
            amount
        }
        val user = retrieveUserByArgsNMessage(context, 0) ?: return
        val balance = balanceWrapper.getBalance(user.idLong)
        if (user.idLong == context.authorId) {
            val msg = context.getTranslation("$root.payself")
            sendRsp(context, msg)
            return
        }
        balanceWrapper.setBalance(context.authorId, cash - amount)
        balanceWrapper.setBalance(user.idLong, balance + amount)
        val msg = context.getTranslation("$root.payed")
            .withVariable("user", user.asTag)
            .withVariable("amount", amount)
            .withVariable("balance", cash - amount)
        sendRsp(context, msg)
    }
}