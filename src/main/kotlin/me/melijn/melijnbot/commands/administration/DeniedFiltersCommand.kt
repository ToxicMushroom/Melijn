package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.utils.*

class DeniedFiltersCommand : AbstractCommand("command.deniedfilters") {

    init {
        id = 115
        name = "deniedFilters"
        children = arrayOf(AddArg(root), RemoveArg(root), RemoveByIndexArg(root), ListArg(root))
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            val channel = getTextChannelByArgsN(context, 0)
            val denied = if (channel == null) {
                context.rawArg
            } else {
                context.rawArg.replaceFirst((context.args[0] + "\\s+").toRegex(), "")
            }
            val wrapper = context.daoManager.filterWrapper
            wrapper.addFilter(context.guildId, channel?.idLong, FilterType.DENIED, denied)

            val part = if (channel == null) "" else ".channel"
            val msg = context.getTranslation("$root.success$part")
                .replace("%filter%", denied)
                .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "")
            sendMsg(context, msg)
        }
    }


    class RemoveByIndexArg(parent: String) : AbstractCommand("$parent.removebyindex") {

        init {
            name = "removeByIndex"
            aliases = arrayOf("removeAt", "rbi", "deleteByIndex")
        }

        override suspend fun execute(context: CommandContext) {
            val channel = getTextChannelByArgsN(context, 0)
            val wrapper = context.daoManager.filterWrapper
            val filters = wrapper.deniedFilterCache.get(Pair(context.guildId, channel?.idLong)).await()
            val index = getIntegerFromArgNMessage(context, context.args.size - 1, 0, filters.size - 1) ?: return
            val denied = filters[index]
            wrapper.removeFilter(context.guildId, channel?.idLong, FilterType.DENIED, denied)

            val part = if (channel == null) "" else ".channel"
            val msg = context.getTranslation("$root.success$part")
                .replace("%filter%", denied)
                .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "")
            sendMsg(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "delete", "d")
        }

        override suspend fun execute(context: CommandContext) {
            val channel = getTextChannelByArgsN(context, 0)
            val denied = if (channel == null) {
                context.rawArg
            } else {
                context.rawArg.replaceFirst((context.args[0] + "\\s+").toRegex(), "")
            }

            val wrapper = context.daoManager.filterWrapper
            wrapper.removeFilter(context.guildId, channel?.idLong, FilterType.DENIED, denied)

            val part = if (channel == null) "" else ".channel"
            val msg = context.getTranslation("$root.success$part")
                .replace("%filter%", denied)
                .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "")
            sendMsg(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val channel = getTextChannelByArgsN(context, 0)
            val wrapper = context.daoManager.filterWrapper
            val filters = wrapper.deniedFilterCache.get(Pair(context.guildId, channel?.idLong)).await()

            val part = if (channel == null) "" else ".channel"
            val title = context.getTranslation("$root.success$part")
                .replace(PLACEHOLDER_CHANNEL, channel?.asTag ?: "error")
            var content = "```INI"
            for ((index, filter) in filters.withIndex()) {
                content += "\n$index - [$filter]"
            }
            content += "```"
            val msg = title + content
            sendMsg(context, msg)
        }
    }
}