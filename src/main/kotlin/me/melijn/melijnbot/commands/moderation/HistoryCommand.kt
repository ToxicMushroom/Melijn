package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendPaginationMsg
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withVariable

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
                .withVariable(PLACEHOLDER_ARG, context.args[0])
            sendRsp(context, msg)
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
                .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                .withVariable("typeList", readableList)

            sendRsp(context, noHistory)
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
                                    .withVariable("page", countr++)
                                    .withVariable("pages", pages)
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
                        .withVariable("page", countr)
                        .withVariable("pages", pages)
                    )
                    paginationPartList.add(paginationPart.toString())
                    paginationPart.clear()
                }

                sendPaginationMsg(context, paginationPartList, 0)
            } else {
                sendRsp(context, msg)
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
            sendRspCodeBlock(context, msg, "INI")
        }
    }
}