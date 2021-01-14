package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withVariable

class BalanceCommand : AbstractCommand("command.balance") {

    init {
        id = 190
        name = "balance"
        aliases = arrayOf("bal", "money")
        commandCategory = CommandCategory.ECONOMY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            val balance = context.daoManager.balanceWrapper.getBalance(context.authorId)
            val description = context.getTranslation("$root.show.self")
                .withVariable("user", context.author.asTag)
                .withVariable("balance", balance)

            val eb = Embedder(context)
                .setDescription(description)
            sendEmbedRsp(context, eb.build())

        } else {
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val balance = context.daoManager.balanceWrapper.getBalance(user.idLong)
            val description = context.getTranslation("$root.show.other")
                .withVariable("user", user.asTag)
                .withVariable("balance", balance)

            val eb = Embedder(context)
                .setDescription(description)
            sendEmbedRsp(context, eb.build())
        }
    }
}