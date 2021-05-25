package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.enums.Alignment
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.Cell
import me.melijn.melijnbot.internals.utils.TableBuilder
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import java.text.NumberFormat
import java.util.*
import javax.swing.text.NumberFormatter

val bigNumberFormatter = NumberFormatter(NumberFormat.getInstance(Locale.UK))

class ShardsCommand : AbstractCommand("command.cluster") {

    init {
        id = 11
        name = "cluster"
        aliases = arrayOf("shardList", "shards")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val shardManager = context.shardManager
        val tableBuilder = TableBuilder().apply {
            this.setColumns(
                Cell("Shard"),
                Cell("Ping", Alignment.RIGHT),
                Cell("Users", Alignment.RIGHT),
                Cell("Guilds", Alignment.RIGHT),
                Cell("VCs", Alignment.RIGHT)
            )
            this.footerTopSeperator = "═"
        }

        var averagePing = 0L
        var totalVCs = 0L
        for (shard in shardManager.shardCache.reversed()) {
            shard?.let { jda ->
                val shardId = jda.shardInfo.shardId
                val shardInfo = if (context.jda.shardInfo.shardId == shardId) {
                    " ←"
                } else {
                    ""
                }
                val vcs: Long = jda.voiceChannels.stream().filter { vc ->
                    vc.members.contains(vc.guild.selfMember)
                }.count()

                averagePing += jda.gatewayPing
                totalVCs += vcs
                tableBuilder.addRow(
                    Cell("$shardId.$shardInfo"),
                    Cell(bigNumberFormatter.valueToString(jda.gatewayPing), Alignment.RIGHT),
                    Cell(bigNumberFormatter.valueToString(jda.userCache.size()), Alignment.RIGHT),
                    Cell(bigNumberFormatter.valueToString(jda.guildCache.size()), Alignment.RIGHT),
                    Cell(bigNumberFormatter.valueToString(vcs), Alignment.RIGHT)
                )
            }

        }
        averagePing /= shardManager.shardsTotal

        tableBuilder.setFooterRow(
            Cell("Sum/Avg"),
            Cell(averagePing.toString(), Alignment.RIGHT),
            Cell(shardManager.userCache.size().toString(), Alignment.RIGHT),
            Cell(shardManager.guildCache.size().toString(), Alignment.RIGHT),
            Cell(totalVCs.toString(), Alignment.RIGHT)
        )

        for (part in tableBuilder.build(true)) {
            val eb = Embedder(context)
                .setDescription(part)
            sendEmbedRsp(context, eb.build())
        }
    }
}