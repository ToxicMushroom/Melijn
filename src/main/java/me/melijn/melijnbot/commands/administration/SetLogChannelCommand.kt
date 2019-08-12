package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.utils.MarkdownUtil

class SetLogChannelCommand : AbstractCommand("command.setlogchannel") {

    init {
        id = 21
        name = "setLogChannel"
        aliases = arrayOf("slc")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val matchingEnums: List<LogChannelType> = LogChannelType.getMatchingTypesFromNode(context.args[0])
        if (matchingEnums.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        handleEnums(context, matchingEnums)
    }

    private fun handleEnums(context: CommandContext, logChannelTypes: List<LogChannelType>) {
        if (context.args.size > 1) {
            setChannels(context, logChannelTypes)
        } else {
            displayChannels(context, logChannelTypes)
        }
    }

    private fun displayChannels(context: CommandContext, logChannelTypes: List<LogChannelType>) {
        val daoWrapper = context.daoManager.logChannelWrapper
        val title = Translateable("$root.settings").string(context)
                .replace("%logChannelTypeNode%", context.args[0])
        val lines = emptyList<String>().toMutableList()
        val eb = Embedder(context)
        for (type in logChannelTypes) {
            val pair = Pair(context.guildId, type)
            val channelId = daoWrapper.logChannelCache.get(pair).get()
            val channel = context.getGuild().getTextChannelById(channelId)

            if (channelId != -1L && channel == null) daoWrapper.removeChannel(pair.first, pair.second)
            lines += MarkdownUtil.bold(type.text) + " " + (channel?.asMention ?: "/")
        }
        val content = lines.joinToString("\n")
        eb.addField(title, content, false)
        sendEmbed(context, eb.build())
    }

    private fun setChannels(context: CommandContext, logChannelTypes: List<LogChannelType>) {
        if (context.args.size < 2) {
            sendSyntax(context, syntax)
            return
        }

        val daoWrapper = context.daoManager.logChannelWrapper
        val msg =if (context.args[1].equals("null", true)) {

            daoWrapper.removeChannels(context.guildId, logChannelTypes)

            Translateable("$root.unset").string(context)
                    .replace("%channelCount%", logChannelTypes.size.toString())
                    .replace("%logChannelNode%", context.args[0])
        } else {
            val channel = getTextChannelByArgsNMessage(context, 1) ?: return
            daoWrapper.setChannels(context.guildId, logChannelTypes, channel.idLong)

            Translateable("$root.set").string(context)
                    .replace("%channelCount%", logChannelTypes.size.toString())
                    .replace("%logChannelNode%", context.args[0])
                    .replace("%channel%", channel.asTag)

        }
        sendMsg(context, msg)
    }
}