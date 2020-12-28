package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.isInside
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import kotlin.math.abs
import kotlin.random.Random

class FlipXCommand : AbstractCommand("command.flipx") {

    init {
        id = 243
        name = "flipx"
        commandCategory = CommandCategory.ECONOMY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.size != 3) {
            sendSyntax(context)
            return
        }

        val balanceWrapper = context.daoManager.balanceWrapper
        val cash = balanceWrapper.getBalance(context.authorId)

        val flipTimes = getIntegerFromArgNMessage(context, 0, 1, 1000) ?: return
        val amount = getLongFromArgNMessage(context, 1, 1) ?: return
        val bet = amount * flipTimes

        if (bet > cash) {
            val msg = context.getTranslation("$root.bettobig")
                .withVariable("bet", bet)
                .withVariable("cash", cash)
            sendRsp(context, msg)
            return
        }


        val heads = context.getTranslation("command.flip.heads")
        val tails = context.getTranslation("command.flip.tails")
        when {
            context.args[2].isInside("heads", "head", ignoreCase = true) -> {
                flipCoin(context, amount, cash, heads, tails, 1, flipTimes)
            }
            context.args[2].isInside("tails", "tail", ignoreCase = true) -> {
                flipCoin(context, amount, cash, tails, heads, 0, flipTimes)
            }
            else -> {
                sendSyntax(context)
                return
            }
        }
    }

    private suspend fun flipCoin(
        context: ICommandContext,
        bet: Long,
        cash: Long,
        winning: String,
        losing: String,
        winCon: Int,
        flipTimes: Int
    ) {
        var wins = 0L
        var difference = 0L
        val balanceWrapper = context.daoManager.balanceWrapper

        var seed = System.currentTimeMillis()
        for (i in 0 until flipTimes) {
            val nextInt = Random(seed).nextInt(2)
            seed += 111
            if (nextInt == winCon) {
                wins++
                difference += bet
            } else {
                difference -= bet
            }
        }

        balanceWrapper.setBalance(context.authorId, cash + difference)

        val msg = context.getTranslation(
            "$root." + when {
                difference > 0 -> "won"
                difference < 0 -> "lost"
                else -> "even"
            }
        ).withVariable("wins", wins)
            .withVariable("losses", flipTimes - wins)
            .withVariable("winningSide", winning)
            .withVariable("losingSide", losing)
            .withVariable("diff", abs(difference))
            .withVariable("cash", cash + difference)
        sendRsp(context, msg)

    }
}