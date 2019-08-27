package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgCodeBlocks
import me.melijn.melijnbot.objects.utils.sendSyntax

class HistoryCommand : AbstractCommand("command.history") {

    init {
        id = 31
        name = "history"
        commandCategory = CommandCategory.MODERATION
    }

    override fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context, syntax)
            return
        }
        val types = PunishmentType.getMatchingTypesFromNode(context.args[0])
        if (types.isEmpty()) {
            val msg = Translateable("message.unknown.punishmenttype").string(context)
                    .replace("%arg%", context.args[0])
            sendMsg(context, msg)
            return
        }

        retrieveUserByArgsNMessage(context, 1) { targetUser ->
            if (targetUser == null) return@retrieveUserByArgsNMessage
            val unorderedMap: MutableMap<Long, String> = hashMapOf()
            val shardManager = context.getShardManager() ?: return@retrieveUserByArgsNMessage
            val dao = context.daoManager
            val guildId = context.getGuildId()


            var counter = 0
            val mapHandler: (Map<Long, String>) -> Unit = { map ->
                unorderedMap.putAll(map)
                counter++
                if (counter == types.size) {
                    val orderedMap = unorderedMap.toSortedMap().toMap()

                    //Collected all punishments
                    val msg = orderedMap.values.joinToString("")

                    if (msg.isBlank()) {
                        val or = Translateable("or").string(context)
                        var readableList = types.subList(0, types.size - 1).joinToString(", ", transform = { type ->
                            type.name.toLowerCase()
                        })
                        if (readableList.isBlank()) {
                            readableList = types.last().name.toLowerCase()
                        } else {
                            readableList += " $or " + types.last().name.toLowerCase()
                        }

                        val noHistory = Translateable("$root.nohistory").string(context)
                                .replace(PLACEHOLDER_USER, targetUser.asTag)
                                .replace("%typeList%", readableList)
                        sendMsg(context, noHistory)
                    } else {
                        sendMsgCodeBlocks(context, msg, "INI")
                    }
                }
            }
            for (type in types) {
                when (type) {
                    PunishmentType.BAN -> {
                        dao.banWrapper.getBanMap(shardManager, guildId, targetUser, mapHandler)
                    }
                    PunishmentType.KICK -> {
                        dao.kickWrapper.getKickMap(shardManager, guildId, targetUser, mapHandler)
                    }
                    PunishmentType.WARN -> {
                        dao.warnWrapper.getWarnMap(shardManager, guildId, targetUser, mapHandler)
                    }
                    PunishmentType.MUTE -> {
                        dao.muteWrapper.getMuteMap(shardManager, guildId, targetUser, mapHandler)
                    }
                }
            }
        }
    }
}