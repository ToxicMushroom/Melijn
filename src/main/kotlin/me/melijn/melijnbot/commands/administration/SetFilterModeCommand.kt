package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.entities.TextChannel

const val UNKNOWN_WRAPPINGMODE_PATH: String = "message.unknown.filtermode"

class SetFilterModeCommand : AbstractCommand("command.setfiltermode") {

    init {
        id = 113
        name = "setFilterMode"
        aliases = arrayOf("sfm")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            showFilterMode(context, null)
        } else if (context.args.size == 1) {
            val firstArg = context.args[0]
            if (firstArg == "null") {
                unSetFilterMode(context, null)
            } else if (CHANNEL_MENTION.matcher(firstArg).matches() || firstArg.isNumber()) {
                val textChannel = getTextChannelByArgsNMessage(context, 0) ?: return
                showFilterMode(context, textChannel)
            } else {
                val mode: FilterMode = getEnumFromArgNMessage(context, 0, UNKNOWN_WRAPPINGMODE_PATH) ?: return
                setFilterMode(context, null, mode)
            }
        } else if (context.args.size == 2) {
            val secondArg = context.args[1]
            val textChannel = getTextChannelByArgsNMessage(context, 0) ?: return
            if (secondArg == "null") {
                unSetFilterMode(context, textChannel)
            } else {
                val mode: FilterMode = getEnumFromArgNMessage(context, 1, UNKNOWN_WRAPPINGMODE_PATH) ?: return
                setFilterMode(context, null, mode)
            }
        } else {
            sendSyntax(context)
        }
    }

    private suspend fun setFilterMode(context: CommandContext, channel: TextChannel?, mode: FilterMode) {
        val channelId = channel?.idLong
        val wrapper = context.daoManager.filterModeWrapper
        wrapper.setMode(context.guildId, channelId, mode)
        val part = if (channelId == null) "" else ".channel"
        val msg = i18n.getTranslation(context, "$root.set$part")
            .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "error")
            .replace("%mode%", mode.toUCSC())
        sendMsg(context, msg)
    }

    private suspend fun unSetFilterMode(context: CommandContext, channel: TextChannel?) {
        val channelId = channel?.idLong
        val wrapper = context.daoManager.filterModeWrapper
        wrapper.unsetMode(context.guildId, channelId)
        val part = if (channelId == null) "" else ".channel"
        val msg = i18n.getTranslation(context, "$root.unset$part")
            .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "error")
        sendMsg(context, msg)
    }


    private suspend fun showFilterMode(context: CommandContext, channel: TextChannel?) {
        val channelId = channel?.idLong
        val wrapper = context.daoManager.filterModeWrapper
        val mode = wrapper.filterWrappingModeCache.get(Pair(context.guildId, channelId)).await()
        val part = if (channelId == null) "" else ".channel"
        val msg = i18n.getTranslation(context, "$root.show$part")
            .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "error")
            .replace("%mode%", mode.toUCSC())
        sendMsg(context, msg)
    }
}