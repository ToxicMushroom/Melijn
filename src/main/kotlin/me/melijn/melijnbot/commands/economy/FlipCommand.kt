package me.melijn.melijnbot.commands.economy

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.isInside
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import kotlin.random.Random

class FlipCommand : AbstractCommand("command.flip") {

    init {
        id = 192
        name = "flip"
        aliases = arrayOf("coinflip")
        commandCategory = CommandCategory.ECONOMY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size != 2) {
            sendSyntax(context)
            return
        }

        val balanceWrapper = context.daoManager.balanceWrapper
        val cash = balanceWrapper.balanceCache.get(context.authorId).await()

        val amount = if (context.args[0].equals("all", true)) {
            cash
        } else {
            val amount = getLongFromArgNMessage(context, 0, 1) ?: return
            if (amount > cash) {
                val msg = context.getTranslation("$root.bettobig")
                    .withVariable("bet", amount)
                    .withVariable("cash", cash)
                sendRsp(context, msg)
                return
            }
            amount
        }

        val heads = context.getTranslation("$root.heads")
        val tails = context.getTranslation("$root.tails")
        when {
            context.args[1].isInside("heads", "head", ignoreCase = true) -> {
                flipCoin(context, amount, cash, heads, tails, 1)
            }
            context.args[1].isInside("tails", "tail", ignoreCase = true) -> {
                flipCoin(context, amount, cash, tails, heads, 0)
            }
            else -> {
                sendSyntax(context)
                return
            }
        }
    }

    private suspend fun flipCoin(context: CommandContext, bet: Long, cash: Long, winning: String, losing: String, winCon: Int) {
        val nextInt = Random.nextInt(2)
        val balanceWrapper = context.daoManager.balanceWrapper


        if (nextInt == winCon) {
            val newCash = bet + cash
            balanceWrapper.setBalance(context.authorId, newCash)

            val msg = context.getTranslation("$root.won")
                .withVariable("amount", bet)
                .withVariable("cash", newCash)
                .withVariable("landed", winning)
            sendRsp(context, msg)

        } else {
            val newCash = cash - bet
            balanceWrapper.setBalance(context.authorId, newCash)

            val msg = context.getTranslation("$root.lost")
                .withVariable("amount", bet)
                .withVariable("cash", newCash)
                .withVariable("landed", losing)
            sendRsp(context, msg)
        }
    }
}