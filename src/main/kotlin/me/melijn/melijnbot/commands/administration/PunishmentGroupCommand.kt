package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.autopunishment.PunishGroup
import me.melijn.melijnbot.enums.PointsTriggerType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.*

const val UNKNOWN_POINTSTRIGGERTTYPE_PATH: String = "message.unknown.pointstriggertype"


class PunishmentGroupCommand : AbstractCommand("command.punishmentgroup") {

    init {
        id = 124
        name = "punishmentGroup"
        aliases = arrayOf("pg", "PGroup", "punishmentG", "punishG")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            SelectArg(root),
            ListArg(root),
            SetPPTriggerArg(root),
            SetPPGoalArg(root),
            RemovePPGoalArg(root)
        )
        commandCategory = CommandCategory.DEVELOPER
    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, String>()
        suspend fun getSelectedPGroup(context: CommandContext): PunishGroup? {
            val pair = Pair(context.guildId, context.authorId)
            return if (selectionMap.containsKey(pair)) {
                val id = selectionMap[pair] ?: return null
                val pList = context.daoManager.autoPunishmentGroupWrapper.autoPunishmentCache.get(context.guildId).await()
                val punishGroup = pList.firstOrNull { (groupName) ->
                    groupName == id
                }

                if (punishGroup == null) {
                    val msg = context.getTranslation("message.pgremoved")
                        .replace(PREFIX_PLACE_HOLDER, context.usedPrefix)
                    sendMsg(context, msg)
                }
                punishGroup
            } else {
                val msg = context.getTranslation("message.nopgselected")
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
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val name = getStringFromArgsNMessage(context, 0, 1, 64, cantContainChars = arrayOf('[', ',', ']'))
                ?: return
            wrapper.add(context.guildId, name)

            val msg = context.getTranslation("$root.added")
                .replace("%name%", name)
            sendMsg(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val name = context.args[0]
            wrapper.remove(context.guildId, name)

            val msg = context.getTranslation("$root.removed")
                .replace("%name%", name)
            sendMsg(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val list = wrapper.getMapsForGuild(context.guildId)

            val title = context.getTranslation("$root.title")
            val ppTrigger = context.getTranslation("$root.pptrigger")
            val ppGoal = context.getTranslation("$root.ppgoal")
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

            sendMsgCodeBlock(context, msg, "INI")
        }
    }

    class SelectArg(parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.autoPunishmentGroupWrapper
            val group = getStringFromArgsNMessage(context, 0, 1, 64, cantContainChars = arrayOf('[', ',', ']'))
                ?: return
            val punishGroup = wrapper.autoPunishmentCache[context.guildId].await().firstOrNull { (groupName) ->
                groupName == group
            }
            if (punishGroup == null) {
                val msg = context.getTranslation("message.unknown.punishgroup")
                    .replace(PLACEHOLDER_ARG, group)
                sendMsg(context, msg)
                return
            }

            selectionMap[Pair(context.guildId, context.authorId)] = group

            val msg = context.getTranslation("$root.selected")
                .replace("%group%", group)
            sendMsg(context, msg)
        }
    }

    class SetPPTriggerArg(parent: String) : AbstractCommand("$parent.setpunishmentpointtrigger") {

        init {
            name = "setPunishmentPointTrigger"
            aliases = arrayOf("setPPTrigger")
        }

        override suspend fun execute(context: CommandContext) {
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
                .replace("%group%", pg.groupName)
                .replace("%type%", type.name)
            sendMsg(context, msg)
        }
    }

    class SetPPGoalArg(parent: String) : AbstractCommand("$parent.setpunishmentpointsgoal") {

        init {
            name = "setPunishmentPointsGoal"
            aliases = arrayOf("sppg", "sppgoal", "setPPGoal")
        }

        override suspend fun execute(context: CommandContext) {
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
                .replace("%groupName%", pg.groupName)
                .replace("%points%", points.toString())
                .replace("%punishment%", punishment.name)

            sendMsg(context, msg)
        }
    }

    class RemovePPGoalArg(parent: String) : AbstractCommand("$parent.removepunishmentpointsgoal") {

        init {
            name = "removePunishmentPointsGoal"
            aliases = arrayOf("rppg", "rppgoal")
        }

        override suspend fun execute(context: CommandContext) {
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
                    .replace("%groupName%", pg.groupName)
                    .replace("%points%", points.toString())
                sendMsg(context, msg)
                return
            }

            map.remove(points)
            wrapper.setPointGoalMap(context.guildId, pg.groupName, map)

            val msg = context.getTranslation("$root.removed")
                .replace("%groupName%", pg.groupName)
                .replace("%points%", points.toString())
                .replace("%punishment%", punishment)

            sendMsg(context, msg)
        }
    }

    class CopyArg(parent: String) : AbstractCommand("$parent.copy") {

        init {
            name = "copy"
        }

        override suspend fun execute(context: CommandContext) {
            sendMsg(context, "not implemented yet")
        }
    }
}