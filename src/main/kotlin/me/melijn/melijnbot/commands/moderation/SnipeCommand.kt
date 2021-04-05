package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.events.eventlisteners.MessageDeletedListener
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp

class SnipeCommand : AbstractCommand("command.snipe") {

    init {
        id = 221
        name = "snipe"
        aliases = arrayOf("recentDeletions")
        commandCategory = CommandCategory.MODERATION
    }

    suspend fun execute(context: ICommandContext) {
        val amount: Int
        val channelId: Long
        val guildId = context.guildId
        when (context.args.size) {
            1 -> {
                amount = if (context.args[0] == "all") {
                    10
                } else {
                    getIntegerFromArgN(context, 0, 1, 10) ?: return
                }

                channelId = context.channelId
            }
            2 -> {
                amount = if (context.args[0] == "all") {
                    10
                } else {
                    getIntegerFromArgN(context, 0, 1, 10) ?: return
                }

                channelId = getTextChannelByArgsN(context, 1, true)?.idLong ?: return


            }
            else -> {
                amount = 1
                channelId = context.channelId
            }
        }

        val locId = Pair(guildId, channelId)
        val snipeLog = MessageDeletedListener.recentDeletions[locId]
        if (snipeLog == null || snipeLog.isEmpty()) {
            val msg = context.getTranslation("$root.nosnipe")
            sendRsp(context, msg)
            return
        }

        val log = snipeLog.entries.sortedBy { it.value }.reversed().take(amount)
        val newLog = snipeLog.toMutableMap()
        log.forEach { newLog.remove(it.key) }
        MessageDeletedListener.recentDeletions[locId] = newLog

        val sb = StringBuilder()
        for ((key) in log.sortedBy { it.key.moment }) {
            sb.append("[")
                .append(key.moment.asEpochMillisToDateTime(context.getTimeZoneId()))
                .append("] **")
                .append(
                    context.guild.retrieveMemberById(key.authorId).awaitOrNull()?.asTag?.escapeMarkdown()
                        ?: key.authorId
                )
                .append("**: ")
                .appendLine(key.content.escapeMarkdown())
        }
        sendRsp(context, sb.toString())


    }
}