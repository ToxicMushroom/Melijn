package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import kotlin.random.Random

class DiceCommand : AbstractCommand("command.dice") {

  init {
    name = "dice"
    aliases = arrayOf("throwDice")
    commandCategory = CommandCategory.UTILITY
  }

  override suspend fun execute(context: ICommandContext) {
    when {
      context.args.isEmpty() -> throwDice(context, 6, 1)
      context.args.size == 1 -> {
        val eyes = getIntegerFromArgNMessage(context, 0, 1) ?: return
        throwDice(context, eyes, 1)
      }
      else -> {
        val eyes = getIntegerFromArgNMessage(context, 0, 1) ?: return
        val dices = getIntegerFromArgNMessage(context, 1, 1, 20) ?: return
        throwDice(context, eyes, dices)
      }
    }
  }

  private suspend fun throwDice(context: ICommandContext, eyes: Int, dices: Int) {
    val throws = IntArray(dices)
    for (n in 0 until dices) {
      val value = Random.nextInt(eyes) + 1
      throws[n] = value
    }

    if (throws.size == 1) {
      sendRsp(context, "Threw a `$eyes-eyed` 🎲, rolled: **${throws[0]}**")
      return
    }

    var msg = "Threw the `$eyes-eyed` \uD83C\uDFB2 $dices times\n"
    for (n in 0 until dices) {
      msg += "Dice `#${n + 1}` rolled: **${throws[n]}**\n"
    }
    msg += "Sum of all rolls = **${throws.sum()}**"
    sendRsp(context, msg)
  }
}