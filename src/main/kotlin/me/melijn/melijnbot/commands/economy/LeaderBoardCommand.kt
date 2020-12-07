package me.melijn.melijnbot.commands.economy

import me.melijn.melijnbot.commands.utility.bigNumberFormatter
import me.melijn.melijnbot.enums.Alignment
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.Cell
import me.melijn.melijnbot.internals.utils.TableBuilder
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable
import kotlin.math.ceil

class LeaderBoardCommand : AbstractCommand("command.leaderboard") {

    init {
        id = 197
        name = "leaderBoard"
        aliases = arrayOf("lb")
        commandCategory = CommandCategory.ECONOMY
    }

    override suspend fun execute(context: CommandContext) {
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

        val pos = if (!userMap.keys.contains(context.authorId)) {
            wrapper.getPosition(context.authorId)
        } else null


        val last = pos?.second == -1L
        if (pos != null && pos.second < 1 + (10 * page) && !last) {
            tableBuilder.addRow(
                Cell("${pos.second}."),
                Cell(bigNumberFormatter.valueToString(pos.first), Alignment.RIGHT),
                Cell(context.author.asTag)
            )
            tableBuilder.addSplit()
        }
        for ((index, pair) in userMap.toList().withIndex()) {
            val user = context.shardManager.retrieveUserById(pair.first).await()

            tableBuilder.addRow(
                Cell("${index + 1 + (10 * page)}."),
                Cell(bigNumberFormatter.valueToString(pair.second), Alignment.RIGHT),
                Cell(user.asTag)
            )

        }
        if (pos != null && (pos.second > 1 + (10 * page) || last)) {
            tableBuilder.addSplit()
            tableBuilder.addRow(
                Cell(if (last) "${rowCount + 1}." else "${pos.second}."),
                Cell(bigNumberFormatter.valueToString(pos.first), Alignment.RIGHT),
                Cell(context.author.asTag)
            )
        }


        val msgs = tableBuilder.build(true)

        val totalPageCount = ceil(rowCount / 10.0).toLong()

        val eb = Embedder(context)
        for (msg in msgs) {
            eb.setDescription(msg)
            eb.setFooter("Page ${page + 1}/$totalPageCount")
            sendEmbedRsp(context, eb.build())
        }
    }
}