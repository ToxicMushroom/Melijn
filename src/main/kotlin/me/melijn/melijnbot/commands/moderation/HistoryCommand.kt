package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.utils.*

class HistoryCommand : AbstractCommand("command.history") {

    init {
        id = 31
        name = "history"
        aliases = arrayOf("punishmentHistory", "ph")
        commandCategory = CommandCategory.MODERATION
        children = arrayOf(
            FindByCaseIdArg(root)
        )
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val types = PunishmentType.getMatchingTypesFromNode(context.args[0])
        if (types.isEmpty()) {
            val msg = context.getTranslation("message.unknown.punishmenttype")
                .replace(PLACEHOLDER_ARG, context.args[0])
            sendMsg(context, msg)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 1) ?: return
        val unorderedMap: MutableMap<Long, String> = hashMapOf()
        val dao = context.daoManager

        //put info inside maps
        for (type in types) {
            when (type) {
                PunishmentType.BAN -> {
                    val banMap = dao.banWrapper.getBanMap(context, targetUser)
                    unorderedMap.putAll(banMap)
                }
                PunishmentType.KICK -> {
                    val kickMap = dao.kickWrapper.getKickMap(context, targetUser)
                    unorderedMap.putAll(kickMap)
                }
                PunishmentType.WARN -> {
                    val warnMap = dao.warnWrapper.getWarnMap(context, targetUser)
                    unorderedMap.putAll(warnMap)
                }
                PunishmentType.MUTE -> {
                    val muteMap = dao.muteWrapper.getMuteMap(context, targetUser)
                    unorderedMap.putAll(muteMap)
                }
                PunishmentType.SOFTBAN -> {
                    val banMap = dao.softBanWrapper.getSoftBanMap(context, targetUser)
                    unorderedMap.putAll(banMap)
                }
                else -> {

                }
            }
        }

        val orderedMap = unorderedMap
            .toSortedMap(Comparator.reverseOrder())
            .toMap()

        //Collected all punishments
        val msg = orderedMap.values.joinToString("")

        if (msg.isBlank()) {
            val or = context.getTranslation("or")
            var readableList = types.subList(0, types.size - 1).joinToString(", ", transform = { type ->
                type.name.toLowerCase()
            })

            if (readableList.isBlank()) {
                readableList = types.last().name.toLowerCase()
            } else {
                readableList += " $or " + types.last().name.toLowerCase()
            }

            val noHistory = context.getTranslation("$root.nohistory")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%typeList%", readableList)

            sendMsg(context, noHistory)
        } else {
            val comment = context.getTranslation("message.pagination")
            val maxLength = 1999 - comment.length
            if (msg.length > 2000) {
                val paginationPartList = mutableListOf<String>()
                val paginationPart = StringBuilder("")
                var countr = 1
                var tempLength = 0
                var pages = 0

                for (part in orderedMap.values) {
                    if (part.length < maxLength) {
                        if (tempLength < maxLength) {
                            if (part.length + tempLength < maxLength) {
                                tempLength += part.length
                            } else {
                                tempLength = part.length
                                pages++
                            }
                        }
                    } else continue
                }
                if (tempLength != 0) pages++

                for (part in orderedMap.values) {
                    if (part.length < maxLength) {
                        if (paginationPart.length < maxLength) {
                            if (part.length + paginationPart.length < maxLength) {
                                paginationPart.append(part)
                            } else {
                                paginationPart.append(comment
                                    .replace("%page%", countr++)
                                    .replace("%pages%", pages)
                                )
                                paginationPartList.add(paginationPart.toString())
                                paginationPart.clear()
                                paginationPart.append(part)
                            }
                        }
                    } else continue
                }
                if (paginationPart.isNotEmpty()) {
                    paginationPart.append(comment
                        .replace("%page%", countr)
                        .replace("%pages%", pages)
                    )
                    paginationPartList.add(paginationPart.toString())
                    paginationPart.clear()
                }

                sendPaginationMsg(context, paginationPartList, 0)
            } else {
                sendMsg(context, msg)
            }
        }
    }

    class FindByCaseIdArg(parent: String) : AbstractCommand("$parent.findbycaseid") {

        init {
            name = "findByCaseId"
            aliases = arrayOf("find")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val id = getStringFromArgsNMessage(context, 0, 6, 15) ?: return
            val dao = context.daoManager

            //timestamp, message
            val unorderedMap: MutableMap<Long, String> = hashMapOf()
            //put info inside maps

            val banMap = dao.banWrapper.getBanMap(context, id)
            unorderedMap.putAll(banMap)

            if (unorderedMap.isEmpty()) {
                val kickMap = dao.kickWrapper.getKickMap(context, id)
                unorderedMap.putAll(kickMap)
            }

            if (unorderedMap.isEmpty()) {
                val warnMap = dao.warnWrapper.getWarnMap(context, id)
                unorderedMap.putAll(warnMap)
            }

            if (unorderedMap.isEmpty()) {
                val muteMap = dao.muteWrapper.getMuteMap(context, id)
                unorderedMap.putAll(muteMap)
            }

            if (unorderedMap.isEmpty()) {
                val softbanMap = dao.softBanWrapper.getSoftBanMap(context, id)
                unorderedMap.putAll(softbanMap)
            }


            //Collected all punishments
            val msg = unorderedMap.values.joinToString("")
            sendMsgCodeBlocks(context, msg, "INI")
        }
    }
}