package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
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

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            val balance = context.daoManager.balanceWrapper.balanceCache[context.authorId].await()
            val description = context.getTranslation("$root.show.self")
                .withVariable("user", context.author.asTag)
                .withVariable("balance", balance)

            val eb = Embedder(context)
                .setDescription(description)
            sendEmbedRsp(context, eb.build())

        } else {
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val balance = context.daoManager.balanceWrapper.balanceCache[user.idLong].await()
            val description = context.getTranslation("$root.show.other")
                .withVariable("user", user.asTag)
                .withVariable("balance", balance)

            val eb = Embedder(context)
                .setDescription(description)
            sendEmbedRsp(context, eb.build())
        }
    }
}