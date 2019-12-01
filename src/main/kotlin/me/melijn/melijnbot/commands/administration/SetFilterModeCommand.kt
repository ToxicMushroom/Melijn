package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.entities.TextChannel

const val UNKNOWN_WRAPPINGMODE_PATH: String = "message.unknown.filtermode"

class SetFilterModeCommand : AbstractCommand("command.setfiltermode") {

    init {
        id = 113
        name = "setFilterMode"
        aliases = arrayOf("sfm")
        children = arrayOf(
            ChannelArg(root),
            GlobalArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
        }
    }

    class GlobalArg(parent: String) : AbstractCommand("$parent.global") {

        init {
            name = "global"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                showFilterMode(context, null)
            } else {
                if (context.args[1] == "null") {
                    unSetFilterMode(context, null)
                    return
                }
                val mode: FilterMode = getEnumFromArgNMessage(context, 0, UNKNOWN_WRAPPINGMODE_PATH) ?: return
                setFilterMode(context, null, mode)
            }
        }
    }

    class ChannelArg(parent: String) : AbstractCommand("$parent.channel") {

        init {
            name = "channel"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val textChannel = getTextChannelByArgsNMessage(context, 0) ?: return
            if (context.args.size == 1) {
                showFilterMode(context, textChannel)
            } else {
                if (context.args[1] == "null") {
                    unSetFilterMode(context, textChannel)
                    return
                }
                val mode: FilterMode = getEnumFromArgNMessage(context, 1, UNKNOWN_WRAPPINGMODE_PATH) ?: return
                setFilterMode(context, textChannel, mode)
            }
        }
    }
}

suspend fun setFilterMode(context: CommandContext, channel: TextChannel?, mode: FilterMode) {
    val channelId = channel?.idLong
    val wrapper = context.daoManager.filterModeWrapper
    wrapper.setMode(context.guildId, channelId, mode)
    val msg = context.getTranslation(context.commandOrder.last().root + ".set")
        .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "error")
        .replace("%mode%", mode.toUCSC())
    sendMsg(context, msg)
}

suspend fun unSetFilterMode(context: CommandContext, channel: TextChannel?) {
    val channelId = channel?.idLong
    val wrapper = context.daoManager.filterModeWrapper
    wrapper.unsetMode(context.guildId, channelId)
    val msg = context.getTranslation(context.commandOrder.last().root + ".unset")
        .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "error")
    sendMsg(context, msg)
}


suspend fun showFilterMode(context: CommandContext, channel: TextChannel?) {
    val channelId = channel?.idLong
    val wrapper = context.daoManager.filterModeWrapper
    val mode = wrapper.filterWrappingModeCache.get(Pair(context.guildId, channelId)).await()
    val msg = context.getTranslation(context.commandOrder.last().root + ".show")
        .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "error")
        .replace("%mode%", mode.toUCSC())
    sendMsg(context, msg)
}