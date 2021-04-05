package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.enums.Alignment
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.Cell
import me.melijn.melijnbot.internals.utils.TableBuilder
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
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

    suspend fun execute(context: ICommandContext) {
        val page = (getIntegerFromArgN(context, 0) ?: 1) - 1

        val wrapper = context.daoManager.voteWrapper
        val userMap = wrapper.getTop(10, page * 10)

        if (userMap.isEmpty()) {
            val msg = context.getTranslation("$root.empty")
                .withVariable("page", page)
            sendRsp(context, msg)
            return
        }
        val rowCount = wrapper.getRowCount()

        val tableBuilder = TableBuilder().apply {
            this.setColumns(
                Cell("#"),
                Cell("Votes", Alignment.RIGHT),
                Cell("User")
            )
            this.seperatorOverrides[0] = " "
        }

        createFancyLeaderboard(userMap, context, { id ->
            wrapper.getPosition(id)
        }, page, tableBuilder, rowCount)
    }

    companion object {
        suspend fun createFancyLeaderboard(
            userMap: Map<Long, Long>,
            context: ICommandContext,
            positionGetter: suspend (Long) -> Pair<Long, Long>?,
            page: Int,
            tableBuilder: TableBuilder,
            rowCount: Long
        ) {
            val pos = if (!userMap.keys.contains(context.authorId)) {
                positionGetter(context.authorId)
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
}