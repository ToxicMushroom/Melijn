package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getLongFromArgNMessage
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SetBalanceCommand:AbstractCommand("command.setbalance") {

    init {
        id=191
        name="setBalance"
        aliases= arrayOf("setBal", "setMoney")
        commandCategory=CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val user= retrieveUserByArgsNMessage(context, 0)?:return
        val amount= getLongFromArgNMessage(context,1)?:return
        context.daoManager.balanceWrapper.setBalance(user.idLong, amount)
        sendMsg(context, "${user.asTag}'s balance has been set to **$amount** mel")
    }
}