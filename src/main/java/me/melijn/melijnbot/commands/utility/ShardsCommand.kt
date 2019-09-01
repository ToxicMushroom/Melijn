package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.TableBuilder
import me.melijn.melijnbot.objects.utils.sendMsg


class ShardsCommand : AbstractCommand("command.shards") {

    init {
        id = 11
        name = "shards"
        aliases = arrayOf("shardList", "listShards")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        context.jda.shardManager?.let { shardManager ->
            val tableBuilder = TableBuilder(true).setColumns(listOf("Shard ID", "Ping", "Users", "Guilds", "VCs"))

            var averagePing = 0L
            var totalVCs = 0L
            for (shard in shardManager.shardCache.reversed()) {
                shard?.let { jda ->
                    val shardId = jda.shardInfo.shardId
                    val shardInfo = if (context.jda.shardInfo.shardId == shardId) " (current)" else ""
                    val vcs: Long = jda.voiceChannels.stream().filter {
                        vc -> vc.members.contains(vc.guild.selfMember)
                    }.count()

                    averagePing += jda.gatewayPing
                    totalVCs += vcs
                    tableBuilder.addRow(
                            listOf(
                                    shardId.toString() + shardInfo,
                                    jda.gatewayPing.toString(),
                                    jda.userCache.size().toString(),
                                    jda.guildCache.size().toString(),
                                    vcs.toString()
                            )
                    )
                }

            }
            averagePing /= shardManager.shardsTotal

            tableBuilder.setFooterRow(
                    listOf(
                            "Sum/Avg",
                            averagePing.toString(),
                            shardManager.userCache.size().toString(),
                            shardManager.guildCache.size().toString(),
                            totalVCs.toString()
                    )
            )

            for (part in tableBuilder.build()) {
                sendMsg(context, part)
            }
        }

        if (context.jda.shardManager == null) {
            sendMsg(context, Translateable("$root.noshardmanager").string(context))
        }
    }
}