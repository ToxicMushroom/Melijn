package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.FilterType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.removeFirst
import me.melijn.melijnbot.internals.utils.withVariable

class FilterCommand : AbstractCommand("command.filter") {

    init {
        id = 114
        name = "filter"
        aliases = arrayOf("f", "cf", "chatFilter", "chatMod")
        children = arrayOf(
            AllowedArg(root),
            DeniedArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class AddArg(parent: String, private val filterType: FilterType) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val filterGroup = getFilterGroupNMessage(context, 0) ?: return
            val filter = context.rawArg.removeFirst(context.args[0]).trim()

            val wrapper = context.daoManager.filterWrapper
            wrapper.addFilter(context.guildId, filterGroup.filterGroupName, filterType, filter)


            val msg = context.getTranslation("$root.success")
                .withVariable("filter", filter)
                .withVariable("filterGroup", filterGroup.filterGroupName)
            sendRsp(context, msg)
        }
    }


    class RemoveByIndexArg(parent: String, private val filterType: FilterType) :
        AbstractCommand("$parent.removebyindex") {

        init {
            name = "removeByIndex"
            aliases = arrayOf("removeAt", "rbi", "deleteByIndex")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val filterGroup = getFilterGroupNMessage(context, 0) ?: return
            val wrapper = context.daoManager.filterWrapper

            val filters = wrapper.getFilters(context.guildId, filterGroup.filterGroupName, filterType)
            val index = getIntegerFromArgNMessage(context, 1, 0, filters.size - 1) ?: return
            val filter = filters[index]
            wrapper.removeFilter(context.guildId, filterGroup.filterGroupName, filterType, filter)

            val msg = context.getTranslation("$root.success")
                .withVariable("index", index.toString())
                .withVariable("filter", filter)
                .withVariable("filterGroup", filterGroup.filterGroupName)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String, private val filterType: FilterType) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "delete", "d")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val filterGroup = getFilterGroupNMessage(context, 0) ?: return
            val filter = context.rawArg.removeFirst(context.args[0]).trim()

            val wrapper = context.daoManager.filterWrapper
            wrapper.removeFilter(context.guildId, filterGroup.filterGroupName, filterType, filter)


            val msg = context.getTranslation("$root.success")
                .withVariable("filter", filter)
                .withVariable("filterGroup", filterGroup.filterGroupName)
            sendRsp(context, msg)
        }
    }


    class ListArg(parent: String, private val filterType: FilterType) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val filterGroup = getFilterGroupNMessage(context, 0) ?: return

            val filterWrapper = context.daoManager.filterWrapper
            val filters = filterWrapper.getFilters(context.guildId, filterGroup.filterGroupName, filterType)

            val title = context.getTranslation("$root.success")
                .withVariable("filterGroup", filterGroup.filterGroupName)

            var content = "```INI"
            for ((index, filter) in filters.withIndex()) {
                content += "\n$index - [$filter]"
            }
            content += "```"
            val msg = title + content
            sendRsp(context, msg)
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

        override suspend fun execute(context: ICommandContext) {
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

        override suspend fun execute(context: ICommandContext) {
            sendSyntax(context)
        }
    }
}