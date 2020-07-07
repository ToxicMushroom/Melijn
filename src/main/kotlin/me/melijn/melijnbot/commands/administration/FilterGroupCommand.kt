package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.filter.FilterGroup
import me.melijn.melijnbot.enums.FilterMode
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.utils.*
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.message.sendSyntax
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
            SelectArg(root),
            SetTriggerPoints(root),
            AddChannelArg(root),
            RemoveChannelArg(root),
            ListChannelsArg(root),
            SetStateArg(root),
            SetMode(root)
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

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("put", "a")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 32) ?: return
            val points = getIntegerFromArgN(context, 1, 0) ?: 0
            val mode: FilterMode = getEnumFromArgN(context, 2) ?: FilterMode.DEFAULT
            val state = getBooleanFromArgN(context, 3) ?: true

            val newGroup = FilterGroup(name, state, longArrayOf(), mode, points)
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

        override suspend fun execute(context: CommandContext) {
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

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.filterGroupWrapper
            val groups = wrapper.filterGroupCache.get(context.guildId).await()

            val title = context.getTranslation("$root.title")
            val enabled = context.getTranslation("enabled")
            val disabled = context.getTranslation("disabled")


            var content = "```INI\n[name] - [points] - [state] - [mode]\n  [\n    - channels\n  ]"

            for ((filterGroupName, state, channels, mode, points) in groups) {
                content += "\n[${filterGroupName}] - $points - ${if (state) enabled else disabled} - $mode\n  [\n" +
                    if (channels.isEmpty()) {
                        "    - *"
                    } else {
                        channels.joinToString("\n    - ")
                    } +
                    "\n  ]"
            }
            content += "```"

            val msg = title + content
            sendRsp(context, msg)
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
                .withVariable("filterGroupName", group.filterGroupName)
            sendRsp(context, msg)
        }
    }

    class SetTriggerPoints(parent: String) : AbstractCommand("$parent.settriggerpoints") {

        init {
            name = "setTriggerPoints"
            aliases = arrayOf("stp")
        }

        override suspend fun execute(context: CommandContext) {
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

    class AddChannelArg(parent: String) : AbstractCommand("$parent.addchannel") {

        init {
            name = "addChannel"
            aliases = arrayOf("ac")
        }

        override suspend fun execute(context: CommandContext) {
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

    class RemoveChannelArg(parent: String) : AbstractCommand("$parent.removechannel") {

        init {
            name = "removeChannel"
            aliases = arrayOf("rc")
        }

        override suspend fun execute(context: CommandContext) {
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

    class ListChannelsArg(parent: String) : AbstractCommand("$parent.listchannels") {

        init {
            name = "listChannels"
            aliases = arrayOf("lc")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val group = getSelectedFilterGroup(context) ?: return
            val channelIds = group.channels.toMutableList()
            val channels = mutableListOf<TextChannel>()

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

    class SetStateArg(parent: String) : AbstractCommand("$parent.setstate") {

        init {
            name = "setState"
            aliases = arrayOf("ss")
        }

        override suspend fun execute(context: CommandContext) {
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

        override suspend fun execute(context: CommandContext) {
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

suspend fun getFilterGroupNMessage(context: CommandContext, position: Int): FilterGroup? {
    val arg = context.args[position]
    val filterGroups = context.daoManager.filterGroupWrapper.filterGroupCache.get(context.guildId).await()
    val filterGroup = filterGroups.firstOrNull { (filterGroupName) -> filterGroupName == arg }
    if (filterGroup == null) {
        val msg = context.getTranslation("message.unknown.filtergroup")
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }
    return filterGroup
}