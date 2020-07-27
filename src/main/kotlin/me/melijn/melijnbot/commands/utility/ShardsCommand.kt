package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.TableBuilder
import me.melijn.melijnbot.internals.utils.message.sendRsp


class ShardsCommand : AbstractCommand("command.shards") {

    init {
        id = 11
        name = "shards"
        aliases = arrayOf("shardList", "listShards")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val shardManager = context.shardManager
        val tableBuilder = TableBuilder(true)
            .setColumns("Shard ID", "Ping", "Users", "Guilds", "VCs")

        var averagePing = 0L
        var totalVCs = 0L
        for (shard in shardManager.shardCache.reversed()) {
            shard?.let { jda ->
                val shardId = jda.shardInfo.shardId
                val shardInfo = if (context.jda.shardInfo.shardId == shardId) {
                    " (current)"
                } else {
                    ""
                }
                val vcs: Long = jda.voiceChannels.stream().filter { vc ->
                    vc.members.contains(vc.guild.selfMember)
                }.count()

                averagePing += jda.gatewayPing
                totalVCs += vcs
                tableBuilder.addRow(
                    shardId.toString() + shardInfo,
                    jda.gatewayPing.toString(),
                    jda.userCache.size().toString(),
                    jda.guildCache.size().toString(),
                    vcs.toString()
                )
            }

        }
        averagePing /= shardManager.shardsTotal

        tableBuilder.setFooterRow(
            "Sum/Avg",
            averagePing.toString(),
            shardManager.userCache.size().toString(),
            shardManager.guildCache.size().toString(),
            totalVCs.toString()
        )

        for (part in tableBuilder.build()) {
            sendRsp(context, part)
        }
    }
}