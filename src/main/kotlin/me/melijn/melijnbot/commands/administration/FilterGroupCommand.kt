package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.filter.FilterGroup
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.*

class FilterGroupCommand : AbstractCommand("command.filtergroup") {

    init {
        id = 138
        name = "filterGroup"
        aliases = arrayOf("fg")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root),
            SelectArg(root),
            SetTriggerPoints(root),
            AddChannelArg(root),
            RemoveChannelArg(root),
            ListChannelsArg(root),
            SetStateArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, String>()
        suspend fun getSelectedFilterGroup(context: CommandContext): FilterGroup? {
            val pair = Pair(context.guildId, context.authorId)
            return if (selectionMap.containsKey(pair)) {
                val fgn = selectionMap[pair]
                val fgs = context.daoManager.filterGroupWrapper.filterGroupCache.get(context.guildId).await()
                    .filter { (ccId) -> ccId == fgn }
                if (fgs.isNotEmpty()) {
                    fgs[0]
                } else {
                    val msg = context.getTranslation("message.fgremoved")
                        .replace(PREFIX_PLACE_HOLDER, context.usedPrefix)
                    sendMsg(context, msg)
                    null
                }
            } else {
                val msg = context.getTranslation("message.nofgselected")
                    .replace(PREFIX_PLACE_HOLDER, context.usedPrefix)
                sendMsg(context, msg)
                null
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("put", "a")
        }

        override suspend fun execute(context: CommandContext) {
            val name = getStringFromArgsNMessage(context, 0, 1, 32) ?: return
            val points = getIntegerFromArgN(context, 1, 0) ?: 0
            val state = getBooleanFromArgN(context, 2) ?: true

            val newGroup = FilterGroup(name, state, longArrayOf(), points)
            context.daoManager.filterGroupWrapper.putGroup(context.guildId, newGroup)

            val stateM = context.getTranslation(if (state) "enabled" else "disabled")
            val msg = context.getTranslation("$root.added")
                .replace("%filterGroupName%", newGroup.filterGroupName)
                .replace("%state%", stateM)
                .replace("%points%", "$points")
            sendMsg(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val group = getFilterGroupNMessage(context, 0) ?: return
            val wrapper = context.daoManager.filterGroupWrapper

            wrapper.deleteGroup(context.guildId, group)
            val msg = context.getTranslation("$root.deleted")
                .replace("%filterGroupName%", group.filterGroupName)
            sendMsg(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.filterGroupWrapper
            val groups = wrapper.filterGroupCache.get(context.guildId).await()

            val title = context.getTranslation("$root.title")
            val enabled = context.getTranslation("enabled")
            val disabled = context.getTranslation("disabled")

            var content = "```INI\n[name] - [points] - [state]"
            for ((filterGroupName, state, points) in groups) {
                content += "\n[${filterGroupName}] - $points - ${if (state) enabled else disabled}"
            }
            content += "```"

            val msg = title + content
            sendMsg(context, msg)
        }
    }

    class SelectArg(parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val group = getFilterGroupNMessage(context, 0) ?: return
            selectionMap[Pair(context.guildId, context.authorId)] = group.filterGroupName

            val msg = context.getTranslation("$root.selected")
                .replace("%groupName%", group.filterGroupName)
            sendMsg(context, msg)
        }
    }

    class SetTriggerPoints(parent: String) : AbstractCommand("$parent.settriggerpoints") {

        init {
            name = "setTriggerPoints"
            aliases = arrayOf("stp")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class AddChannelArg(parent: String) : AbstractCommand("$parent.addchannel") {

        init {
            name = "addChannel"
            aliases = arrayOf("ac")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class RemoveChannelArg(parent: String) : AbstractCommand("$parent.removechannel") {

        init {
            name = "removeChannel"
            aliases = arrayOf("rc")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class ListChannelsArg(parent: String) : AbstractCommand("$parent.listchannels") {

        init {
            name = "listChannels"
            aliases = arrayOf("lc")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class SetStateArg(parent: String) : AbstractCommand("$parent.setstate") {

        init {
            name = "setState"
            aliases = arrayOf("ss")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}

suspend fun getFilterGroupNMessage(context: CommandContext, position: Int): FilterGroup? {
    val arg = context.args[position]
    val filterGroups = context.daoManager.filterGroupWrapper.filterGroupCache.get(context.guildId).await()
    val filterGroup = filterGroups.firstOrNull { groups -> groups.filterGroupName == arg }
    if (filterGroup == null) {
        val msg = context.getTranslation("message.unknown.filtergroup")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }
    return filterGroup
}