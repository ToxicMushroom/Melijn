package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.filter.FilterGroup
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.TextChannel

const val UNKNOWN_FILTERMODE_PATH: String = "message.unknown.filtermode"

class FilterGroupCommand : AbstractCommand("command.filtergroup") {

    init {
        id = 138
        name = "filterGroup"
        aliases = arrayOf("fg")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root),
            TargetArg(root),
            SelectArg(root),
            SetTriggerPoints(root),
            SetDeleteOnHit(root),
            ChannelsArg(root),
            SetStateArg(root),
            SetMode(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class SetDeleteOnHit(parent: String) : AbstractCommand("$parent.setdeleteonhit") {

        init {
            name = "setDeleteOnHit"
            aliases = arrayOf("sdoh")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val group = getSelectedFilterGroup(context) ?: return
            val state = getBooleanFromArgNMessage(context, 0) ?: return

            group.deleteHit = state

            context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

            val statePath = if (state) "enabled" else "disabled"
            val msg = context.getTranslation("$root.$statePath")
            sendRsp(context, msg)
        }

    }

    class ChannelsArg(parent: String) : AbstractCommand("$parent.channels") {

        init {
            name = "channels"
            aliases = arrayOf("c")
            children = arrayOf(
                AddArg(root),
                RemoveArg(root),
                ListArg(root)
            )
        }

        override suspend fun execute(context: ICommandContext) {
            sendSyntax(context)
        }

        class AddArg(parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: ICommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getSelectedFilterGroup(context) ?: return
                val msg = if (context.args[0] == "all") {
                    val textChannels = context.guild.textChannels.map { it.idLong }
                    group.channels += textChannels
                    context.getTranslation("$root.added.all")

                } else {
                    val textChannel = getTextChannelByArgsNMessage(context, 0) ?: return

                    val channels = group.channels.toMutableList()
                    channels.addIfNotPresent(textChannel.idLong)

                    group.channels = channels.toLongArray()
                    context.getTranslation("$root.added")
                        .withVariable(PLACEHOLDER_CHANNEL, textChannel.asTag)
                }

                context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

                sendRsp(context, msg)
            }
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm")
            }

            override suspend fun execute(context: ICommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getSelectedFilterGroup(context) ?: return
                val textChannel = getTextChannelByArgsNMessage(context, 0) ?: return

                val channels = group.channels.toMutableList()
                channels.remove(textChannel.idLong)

                group.channels = channels.toLongArray()

                context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

                val msg = context.getTranslation("$root.removed")
                    .withVariable(PLACEHOLDER_CHANNEL, textChannel.asTag)

                sendRsp(context, msg)
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: ICommandContext) {
                val group = getSelectedFilterGroup(context) ?: return
                val channelIds = group.channels.toMutableList()
                val channels = mutableListOf<TextChannel>()

                if (channelIds.isEmpty()) {
                    val msg = context.getTranslation("$root.empty")
                    sendRsp(context, msg)
                    return
                }

                val title = context.getTranslation("$root.title")
                var content = "```INI\n[index] - [channelId] - [channelName]"
                for (channelId in channelIds) {
                    val textChannel = context.guild.getTextChannelById(channelId) ?: continue
                    channels.add(textChannel)
                }

                channels.sortBy { it.position }

                for ((index, channel) in channels.withIndex()) {
                    content += "\n$index - [${channel.id}] - [${channel.asTag}]"
                }

                val msg = title + content
                sendRsp(context, msg)
            }
        }

    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, String>()
        suspend fun getSelectedFilterGroup(context: ICommandContext): FilterGroup? {
            val pair = Pair(context.guildId, context.authorId)
            return if (selectionMap.containsKey(pair)) {
                val fgn = selectionMap[pair]
                val fgs = context.daoManager.filterGroupWrapper.getGroups(context.guildId)
                    .filter { (ccId) -> ccId == fgn }
                if (fgs.isNotEmpty()) {
                    fgs[0]
                } else {
                    val msg = context.getTranslation("message.fgremoved")
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                    sendRsp(context, msg)
                    null
                }
            } else {
                val msg = context.getTranslation("message.nofgselected")
                    .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
                null
            }
        }
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("put", "a")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 32) ?: return
            val points = getIntegerFromArgN(context, 1, 0) ?: 0
            val mode: FilterMode = getEnumFromArgN(context, 2) ?: FilterMode.DEFAULT
            val state = getBooleanFromArgN(context, 3) ?: true

            val newGroup = FilterGroup(name, emptyList(), state, longArrayOf(), mode, points, true)
            context.daoManager.filterGroupWrapper.putGroup(context.guildId, newGroup)

            val stateM = context.getTranslation(if (state) "enabled" else "disabled")
            val msg = context.getTranslation("$root.added")
                .withVariable("filterGroupName", newGroup.filterGroupName)
                .withVariable("state", stateM)
                .withVariable("mode", "$mode")
                .withVariable("points", "$points")

            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "rm")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val group = getFilterGroupNMessage(context, 0) ?: return
            val wrapper = context.daoManager.filterGroupWrapper

            wrapper.deleteGroup(context.guildId, group)
            val msg = context.getTranslation("$root.removed")
                .withVariable("filterGroupName", group.filterGroupName)
            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.filterGroupWrapper
            val groups = wrapper.getGroups(context.guildId)

            val title = context.getTranslation("$root.title")
            val enabled = context.getTranslation("enabled")
            val disabled = context.getTranslation("disabled")

            var content =
                "```INI\n[name] - [points] - [state] - [mode] - [delete]\n  [\n    - channels\n  ]\n  [\n    - targets\n  ]\n"

            for ((filterGroupName, fgNames, state, channels, mode, points, deleteHit) in groups) {
                content += "\n[${filterGroupName}] - $points - ${if (state) enabled else disabled} - $mode - $deleteHit\n  [\n" +
                    if (channels.isEmpty()) {
                        "    - *"
                    } else {

                        channels.joinToString("\n    - ", prefix = "    - ")
                    } +
                    "\n  ]\n  [\n" +
                    fgNames.joinToString("\n    - ", "    - ") +
                    "\n  ]"
            }
            content += "```"

            val msg = title + content
            sendRsp(context, msg)
        }
    }

    class TargetArg(parent: String) : AbstractCommand("$parent.target") {

        init {
            name = "target"
            children = arrayOf(
                AddArg(root),
                RemoveArg(root),
                RemoveAtArg(root),
                ListArg(root),
            )
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: ICommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getSelectedFilterGroup(context) ?: return
                val groupNames = group.punishGroupNames.toMutableList()
                if (groupNames.isEmpty()) {
                    val msg = context.getTranslation("$root.empty")
                        .withSafeVariable("filter", group.filterGroupName)
                    sendRsp(context, msg)
                }
                groupNames.sort()

                var listString = "```INI\n"
                for ((index, groupName) in groupNames.withIndex()) {
                    listString += "${index + 1} - $groupName"
                }
                listString += "```"

                val msg = context.getTranslation("$root.title")
                    .withVariable("filter", group.filterGroupName) + listString
                sendRsp(context, msg)
            }
        }

        class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

            init {
                name = "removeAt"
                aliases = arrayOf("rma")
            }

            override suspend fun execute(context: ICommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getSelectedFilterGroup(context) ?: return
                val index = getIntegerFromArgNMessage(context, 0, 1, group.punishGroupNames.size) ?: return
                val groupNames = group.punishGroupNames.toMutableList()
                groupNames.sort()
                val groupName = groupNames.removeAt(index)
                group.punishGroupNames = groupNames

                context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

                val msg = context.getTranslation("$root.removed")
                    .withSafeVariable("filter", group.filterGroupName)
                    .withVariable(PLACEHOLDER_ARG, groupName)
                sendRsp(context, msg)
            }
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm")
            }

            override suspend fun execute(context: ICommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getSelectedFilterGroup(context) ?: return
                val pg = getPunishmentGroupByArgNMessage(context, 0) ?: return
                val groupNames = group.punishGroupNames.toMutableList()
                groupNames.remove(pg.groupName)
                group.punishGroupNames = groupNames

                context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

                val msg = context.getTranslation("$root.removed")
                    .withSafeVariable("filter", group.filterGroupName)
                    .withVariable(PLACEHOLDER_ARG, pg.groupName)
                sendRsp(context, msg)
            }
        }

        class AddArg(parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: ICommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getSelectedFilterGroup(context) ?: return
                val pg = getPunishmentGroupByArgNMessage(context, 0) ?: return
                val groupNames = group.punishGroupNames.toMutableList()
                groupNames.addIfNotPresent(pg.groupName)
                group.punishGroupNames = groupNames

                context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

                val msg = context.getTranslation("$root.added")
                    .withSafeVariable("filter", group.filterGroupName)
                    .withVariable(PLACEHOLDER_ARG, pg.groupName)
                sendRsp(context, msg)
            }
        }

        override suspend fun execute(context: ICommandContext) {
            sendSyntax(context)
        }

    }

    class SelectArg(parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val group = getFilterGroupNMessage(context, 0) ?: return
            selectionMap[Pair(context.guildId, context.authorId)] = group.filterGroupName

            val msg = context.getTranslation("$root.selected")
                .withVariable("filterGroupName", group.filterGroupName)
            sendRsp(context, msg)
        }
    }

    class SetTriggerPoints(parent: String) : AbstractCommand("$parent.settriggerpoints") {

        init {
            name = "setTriggerPoints"
            aliases = arrayOf("stp")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val group = getSelectedFilterGroup(context) ?: return
            val points = getIntegerFromArgNMessage(context, 0) ?: return
            group.points = points

            context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

            val msg = context.getTranslation("$root.set")
                .withVariable(PLACEHOLDER_ARG, "$points")
            sendRsp(context, msg)
        }
    }

    class SetStateArg(parent: String) : AbstractCommand("$parent.setstate") {

        init {
            name = "setState"
            aliases = arrayOf("ss")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val group = getSelectedFilterGroup(context) ?: return
            val state = getBooleanFromArgNMessage(context, 0) ?: return

            group.state = state

            context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

            val statePath = if (state) "enabled" else "disabled"
            val msg = context.getTranslation("$root.$statePath")
            sendRsp(context, msg)
        }
    }

    class SetMode(parent: String) : AbstractCommand("$parent.setmode") {

        init {
            name = "setMode"
            aliases = arrayOf("sm")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val group = getSelectedFilterGroup(context) ?: return
            val mode: FilterMode = getEnumFromArgNMessage(context, 0, UNKNOWN_FILTERMODE_PATH) ?: return

            group.mode = mode

            context.daoManager.filterGroupWrapper.putGroup(context.guildId, group)

            val msg = context.getTranslation("$root.set")
                .withVariable("mode", "$mode")

            sendRsp(context, msg)
        }
    }
}

suspend fun getFilterGroupNMessage(context: ICommandContext, position: Int): FilterGroup? {
    val arg = context.args[position]
    val filterGroups = context.daoManager.filterGroupWrapper.getGroups(context.guildId)
    val filterGroup = filterGroups.firstOrNull { (filterGroupName) -> filterGroupName == arg }
    if (filterGroup == null) {
        val msg = context.getTranslation("message.unknown.filtergroup")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }
    return filterGroup
}