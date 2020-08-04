package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.TableBuilder
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable
import kotlin.math.ceil

class TopVotersCommand : AbstractCommand("command.topvoters") {

    init {
        id = 198
        name = "topVoters"
        aliases = arrayOf("tv")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val page = (getIntegerFromArgN(context, 0) ?: 1) - 1

        val wrapper = context.daoManager.voteWrapper
        val userMap = wrapper.getTop(10, page * 10)

        if (userMap.isEmpty()) {
            val msg = context.getTranslation("$root.empty")
                .withVariable("page", page)
            sendRsp(context, msg)
            return
        }

        val tableBuilder = TableBuilder(true)
        tableBuilder.setColumns("Rank", "User", "Votes")
        for ((index, pair) in userMap.toList().withIndex()) {
            val user = context.shardManager.retrieveUserById(pair.first).await()
            tableBuilder.addRow("${index + 1 + (10 * page)}", "${pair.second}", user.asTag)
        }
        val msgs = tableBuilder.build()

        val totalPageCount = ceil(wrapper.getRowCount() / 10.0).toLong()
        for (msg in msgs) {
            sendRsp(context, msg + "Page ${page + 1}/$totalPageCount")
        }
    }
}