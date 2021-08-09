package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.TableBuilder
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendMsgCodeBlock

class ChannelFlagsCommand : AbstractCommand("command.channelflags") {

    init {
        name = "channelFlags"
        aliases = arrayOf("flags")
        runConditions = arrayOf(RunCondition.GUILD)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val channel = if (context.args.isNotEmpty()) {
            getTextChannelByArgsNMessage(context, 0) ?: return
        } else context.textChannel

        val tb = TableBuilder()
            .setColumns("id", "allowed", "denied", "isRole")

        var msg = "**${channel.asMention} Override Flags**"
        channel.permissionOverrides.sortedBy { it.idLong }.forEach {
            tb.addRow(it.id, it.allowedRaw.toString(), it.deniedRaw.toString(), it.isRoleOverride.toString())
        }

        msg += tb.build(false).first()
        sendMsgCodeBlock(context, msg, "")
    }
}