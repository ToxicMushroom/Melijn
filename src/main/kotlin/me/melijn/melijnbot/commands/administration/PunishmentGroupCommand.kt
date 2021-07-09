package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.autopunishment.PunishGroup
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax

const val UNKNOWN_POINTSTRIGGERTTYPE_PATH: String = "message.unknown.pointstriggertype"

class PunishmentGroupCommand : AbstractCommand("command.punishmentgroup") {

    init {
        id = 124
        name = "punishmentGroup"
        aliases = arrayOf("pg", "PGroup", "punishmentG", "punishG", "punishGroup")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            SelectArg(root),
            ListArg(root),
            SetPointExpireTimeArg(root),
            SetPPTriggerArg(root),
            SetPPGoalArg(root),
            RemovePPGoalArg(root),
            CopyArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, String>()
        suspend fun getSelectedPGroup(context: ICommandContext): PunishGroup? {
            val pair = context.guildId to context.authorId
            return if (selectionMap.containsKey(pair)) {
                val id = selectionMap[pair] ?: return null
                val pList = context.daoManager.autoPunishmentGroupWrapper.getList(context.guildId)
                val punishGroup = pList.firstOrNull { (groupName) ->
                    groupName == id
                }

                if (punishGroup == null) {
                    val msg = context.getTranslation("message.pgremoved")
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                    sendRsp(context, msg)
                }
                punishGroup
            } else {
                val msg = context.getTranslation("message.nopgselected")
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
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val name = getStringFromArgsNMessage(context, 0, 1, 64, cantContainChars = arrayOf('[', ',', ']'))
                ?: return
            wrapper.add(context.guildId, name)

            val msg = context.getTranslation("$root.added")
                .withVariable("name", name)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
        }

        override suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val name = context.args[0]
            wrapper.remove(context.guildId, name)

            val msg = context.getTranslation("$root.removed")
                .withVariable("name", name)
            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val list = wrapper.getMapsForGuild(context.guildId)

            val title = context.getTranslation("$root.title")
            val ppTrigger = context.getTranslation("$root.pptrigger")
            val ppGoal = context.getTranslation("$root.ppgoal")
            if (list.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
                return
            }

            var content = "```INI"
            for ((name, pair) in list) {
                val firstMap = pair.first
                val secondMap = pair.second
                content += "\n[$name]:"
                if (firstMap.isNotEmpty()) content += "\n  $ppTrigger:"
                for (type in firstMap) {
                    content += "\n    - ${type.toUCSC()}"
                }
                if (secondMap.isNotEmpty()) content += "\n  $ppGoal:"
                for ((amount, punishmentName) in secondMap) {
                    content += "\n    [$amount] -> $punishmentName"
                }
            }
            content += "```"
            val msg = title + content

            sendRspCodeBlock(context, msg, "INI")
        }
    }

    class SelectArg(parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: ICommandContext) {
            val group = getPunishmentGroupByArgNMessage(context, 0) ?: return
            selectionMap[context.guildId to context.authorId] = group.groupName

            val msg = context.getTranslation("$root.selected")
                .withVariable("group", group.groupName)
            sendRsp(context, msg)
        }
    }

    class SetPointExpireTimeArg(parent: String) : AbstractCommand("$parent.setpointexpiretime") {

        init {
            name = "setPointExpireTime"
        }

        override suspend fun execute(context: ICommandContext) {
            val pg = getSelectedPGroup(context) ?: return
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            if (context.args.isNotEmpty()) {
                val expireMillis = (getDurationByArgsNMessage(context, 0, context.args.size) ?: return) * 1000
                wrapper.setExpireTime(context.guildId, pg.groupName, expireMillis)

                val msg = context.getTranslation("$root.set")
                    .withVariable("group", pg.groupName)
                    .withVariable("duration", getDurationString(expireMillis))
                sendRsp(context, msg)
            } else {
                val time = wrapper.getList(context.guildId).first { it.groupName == pg.groupName }.expireTime

                val msg = context.getTranslation("$root.get")
                    .withVariable("group", pg.groupName)
                    .withVariable("duration", getDurationString(time))
                sendRsp(context, msg)
            }

        }
    }

    class SetPPTriggerArg(parent: String) : AbstractCommand("$parent.setpunishmentpointtrigger") {

        init {
            name = "setPunishmentPointTrigger"
            aliases = arrayOf("setPPTrigger")
        }

        override suspend fun execute(context: ICommandContext) {
            val pg = getSelectedPGroup(context) ?: return
            val type = getEnumFromArgNMessage<PointsTriggerType>(context, 0, UNKNOWN_POINTSTRIGGERTTYPE_PATH)
                ?: return
            val state = getBooleanFromArgNMessage(context, 1) ?: return
            val types = pg.enabledTypes.toMutableList()
            if (state) {
                types.addIfNotPresent(type)
            } else {
                types.remove(type)
            }

            pg.enabledTypes = types.toList()
            context.daoManager.autoPunishmentGroupWrapper.setTriggerTypes(context.guildId, pg.groupName, types)

            val extra = if (state) "enabled" else "disabled"
            val msg = context.getTranslation("$root.set.$extra")
                .withVariable("group", pg.groupName)
                .withVariable("type", type.name)
            sendRsp(context, msg)
        }
    }

    class SetPPGoalArg(parent: String) : AbstractCommand("$parent.setpunishmentpointsgoal") {

        init {
            name = "setPunishmentPointsGoal"
            aliases = arrayOf("sppg", "sppgoal", "setPPGoal")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val pg = getSelectedPGroup(context) ?: return
            val points = getIntegerFromArgNMessage(context, 0, 1) ?: return
            val punishment = getPunishmentNMessage(context, 1) ?: return

            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val map = pg.pointGoalMap
            map[points] = punishment.name
            wrapper.setPointGoalMap(context.guildId, pg.groupName, map)

            val msg = context.getTranslation("$root.set")
                .withVariable("groupName", pg.groupName)
                .withVariable("points", points.toString())
                .withVariable("punishment", punishment.name)

            sendRsp(context, msg)
        }
    }

    class RemovePPGoalArg(parent: String) : AbstractCommand("$parent.removepunishmentpointsgoal") {

        init {
            name = "removePunishmentPointsGoal"
            aliases = arrayOf("rppg", "rppgoal")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val pg = getSelectedPGroup(context) ?: return
            val points = getIntegerFromArgNMessage(context, 0, 1) ?: return

            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val map = pg.pointGoalMap
            val punishment = map[points]
            if (punishment == null) {
                val msg = context.getTranslation("$root.notanentry")
                    .withVariable("groupName", pg.groupName)
                    .withVariable("points", points.toString())
                sendRsp(context, msg)
                return
            }

            map.remove(points)
            wrapper.setPointGoalMap(context.guildId, pg.groupName, map)

            val msg = context.getTranslation("$root.removed")
                .withVariable("groupName", pg.groupName)
                .withVariable("points", points.toString())
                .withVariable("punishment", punishment)

            sendRsp(context, msg)
        }
    }

    class CopyArg(parent: String) : AbstractCommand("$parent.copy") {
        init {
            name = "copy"
            aliases = arrayOf("cp")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val group = getPunishmentGroupByArgNMessage(context, 0) ?: return
            val newName = getStringFromArgsNMessage(context, 1, 1, 64, cantContainChars = arrayOf('[', ',', ']'))
                ?: return

            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val exists = wrapper.getList(context.guildId).any { (groupName) ->
                groupName == newName
            }

            if (exists) {
                val msg = context.getTranslation("$root.exists")
                    .withVariable(PLACEHOLDER_ARG, newName)
                    .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
                return
            }

            wrapper.add(context.guildId, newName)
            wrapper.setPointGoalMap(context.guildId, newName, group.pointGoalMap)
            wrapper.setTriggerTypes(context.guildId, newName, group.enabledTypes)

            val msg = context.getTranslation("$root.copied")
                .withVariable("source", group.groupName)
                .withVariable(PLACEHOLDER_ARG, newName)

            sendRsp(context, msg)
        }
    }
}

suspend fun getPunishmentGroupByArgNMessage(context: ICommandContext, index: Int): PunishGroup? {
    val wrapper = context.daoManager.autoPunishmentGroupWrapper
    val group = getStringFromArgsNMessage(context, index, 1, 64, cantContainChars = arrayOf('[', ',', ']'))
        ?: return null
    val punishGroup = wrapper.getList(context.guildId).firstOrNull { (groupName) ->
        groupName == group
    }
    if (punishGroup == null) {
        val msg = context.getTranslation("message.unknown.punishgroup")
            .withVariable(PLACEHOLDER_ARG, group)
        sendRsp(context, msg)
    }
    return punishGroup
}