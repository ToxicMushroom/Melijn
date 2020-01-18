package me.melijn.melijnbot.commands.administration

import com.google.common.cache.LoadingCache
import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.removeFirst
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import java.util.concurrent.CompletableFuture

class FilterCommand : AbstractCommand("command.filter") {

    init {
        id = 114
        name = "filter"
        aliases = arrayOf("chatFilter", "chatMod")
        children = arrayOf(
            AllowedArg(root),
            DeniedArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class AddArg(parent: String, private val filterType: FilterType) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val filterGroup = getFilterGroupNMessage(context, 0) ?: return
            val filter = context.rawArg.removeFirst(context.args[0]).trim()

            val wrapper = context.daoManager.filterWrapper
            wrapper.addFilter(context.guildId, filterGroup.filterGroupName, filterType, filter)


            val msg = context.getTranslation("$root.success")
                .replace("%filter%", filter)
                .replace("%filterGroup%", filterGroup.filterGroupName)
            sendMsg(context, msg)
        }
    }


    class RemoveByIndexArg(parent: String, private val filterType: FilterType) : AbstractCommand("$parent.removebyindex") {

        init {
            name = "removeByIndex"
            aliases = arrayOf("removeAt", "rbi", "deleteByIndex")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val filterGroup = getFilterGroupNMessage(context, 0) ?: return
            val wrapper = context.daoManager.filterWrapper
            val cache = getCacheFromFilterType(context.daoManager, filterType)
            val filters = cache.get(Pair(context.guildId, filterGroup.filterGroupName)).await()
            val index = getIntegerFromArgNMessage(context, 1, 0, filters.size - 1) ?: return
            val filter = filters[index]
            wrapper.removeFilter(context.guildId, filterGroup.filterGroupName, filterType, filter)

            val msg = context.getTranslation("$root.success")
                .replace("%index%", index.toString())
                .replace("%filter%", filter)
                .replace("%filterGroup%", filterGroup.filterGroupName)
            sendMsg(context, msg)
        }
    }

    class RemoveArg(parent: String, private val filterType: FilterType) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "delete", "d")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val filterGroup = getFilterGroupNMessage(context, 0) ?: return
            val filter = context.rawArg.removeFirst(context.args[0]).trim()

            val wrapper = context.daoManager.filterWrapper
            wrapper.removeFilter(context.guildId, filterGroup.filterGroupName, filterType, filter)


            val msg = context.getTranslation("$root.success")
                .replace("%filter%", filter)
                .replace("%filterGroup%", filterGroup.filterGroupName)
            sendMsg(context, msg)
        }
    }


    class ListArg(parent: String, private val filterType: FilterType) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val filterGroup = getFilterGroupNMessage(context, 0) ?: return

            val cache = getCacheFromFilterType(context.daoManager, filterType)
            val filters = cache.get(Pair(context.guildId, filterGroup.filterGroupName)).await()


            val title = context.getTranslation("$root.success")
                .replace("%filterGroup%", filterGroup.filterGroupName)

            var content = "```INI"
            for ((index, filter) in filters.withIndex()) {
                content += "\n$index - [$filter]"
            }
            content += "```"
            val msg = title + content
            sendMsg(context, msg)
        }
    }

    class AllowedArg(parent: String) : AbstractCommand("$parent.allowed") {

        init {
            name = "allowed"
            aliases = arrayOf("a")

            val type = FilterType.ALLOWED
            children = arrayOf(
                AddArg(root, type),
                RemoveArg(root, type),
                RemoveByIndexArg(root, type),
                ListArg(root, type)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }
    }

    class DeniedArg(parent: String) : AbstractCommand("$parent.denied") {

        init {
            name = "denied"
            aliases = arrayOf("d")

            val type = FilterType.DENIED
            children = arrayOf(
                AddArg(root, type),
                RemoveArg(root, type),
                RemoveByIndexArg(root, type),
                ListArg(root, type)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }
    }
}

fun getCacheFromFilterType(daoManager: DaoManager, filterType: FilterType): LoadingCache<Pair<Long, String>, CompletableFuture<List<String>>> {
    return when (filterType) {
        FilterType.ALLOWED -> daoManager.filterWrapper.allowedFilterCache
        FilterType.DENIED -> daoManager.filterWrapper.deniedFilterCache
    }
}