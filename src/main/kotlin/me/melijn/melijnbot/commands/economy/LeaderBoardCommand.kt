package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.commands.utility.TopVotersCommand
import me.melijn.melijnbot.enums.Alignment
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.Cell
import me.melijn.melijnbot.internals.utils.TableBuilder
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

class LeaderBoardCommand : AbstractCommand("command.leaderboard") {

    init {
        id = 197
        name = "leaderBoard"
        aliases = arrayOf("lb")
        commandCategory = CommandCategory.ECONOMY
    }

    suspend fun execute(context: ICommandContext) {
        val page = (getIntegerFromArgN(context, 0) ?: 1) - 1

        val wrapper = context.daoManager.balanceWrapper
        val userMap = wrapper.getTop(10, page * 10)
        if (userMap.isEmpty()) {
            val msg = context.getTranslation("$root.empty")
                .withVariable("page", page + 1)
            sendRsp(context, msg)
            return
        }
        val rowCount = wrapper.getRowCount()

        val tableBuilder = TableBuilder().apply {
            this.setColumns(
                Cell("#"),
                Cell("Mel", Alignment.RIGHT),
                Cell("User")
            )
            this.seperatorOverrides[0] = " "
        }

        TopVotersCommand.createFancyLeaderboard(userMap, context, { id ->
            wrapper.getPosition(id)
        }, page, tableBuilder, rowCount)
    }
}