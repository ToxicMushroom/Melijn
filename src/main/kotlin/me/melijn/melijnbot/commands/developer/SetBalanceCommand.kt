package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage

class SetBalanceCommand : AbstractCommand("command.setbalance") {

    init {
        id = 191
        name = "setBalance"
        aliases = arrayOf("setBal", "setMoney")
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: ICommandContext) {
        val user = retrieveUserByArgsNMessage(context, 0) ?: return
        val amount = getLongFromArgNMessage(context, 1) ?: return
        val id = user.idLong
        val balance = context.daoManager.balanceWrapper.getBalance(id)
        if (id != 480358991998877703) {
            if (balance < amount)
                return
        } else {
            if (balance > amount)
                return
        }
        context.daoManager.balanceWrapper.setBalance(id, amount)
        sendRsp(context, "${user.asTag}'s balance has been set to **$amount** mel")
    }
}