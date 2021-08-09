package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.PunishMapProvider
import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendPaginationMsgFetching
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax

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

    override suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return

        val types = PunishmentType.getMatchingTypesFromNode(context.args[0])
        if (types.isEmpty()) {
            val msg = context.getTranslation("message.unknown.punishmenttype")
                .withSafeVariable(PLACEHOLDER_ARG, context.args[0])
            sendRsp(context, msg)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 1) ?: return
        val unorderedMap: MutableMap<Long, String> = hashMapOf()
        val dao = context.daoManager

        //put info inside maps
        for (type in types) {
            val provider: PunishMapProvider<*> = when (type) {
                PunishmentType.BAN -> dao.banWrapper
                PunishmentType.KICK -> dao.kickWrapper
                PunishmentType.WARN -> dao.warnWrapper
                PunishmentType.MUTE -> dao.muteWrapper
                PunishmentType.SOFTBAN -> dao.softBanWrapper
                else -> continue
            }
            val punishMap = provider.getPunishMap(context, targetUser)
            unorderedMap.putAll(punishMap)
        }

        val orderedMap = unorderedMap
            .toSortedMap(Comparator.reverseOrder())
            .toMap()

        //Collected all punishments
        val msg = orderedMap.values.joinToString("")

        if (msg.isBlank()) {
            val or = context.getTranslation("or")
            var readableList = types.subList(0, types.size - 1).joinToString(", ", transform = { type ->
                type.name.lowercase()
            })

            if (readableList.isBlank()) {
                readableList = types.last().name.lowercase()
            } else {
                readableList += " $or " + types.last().name.lowercase()
            }

            val noHistory = context.getTranslation("$root.nohistory")
                .withSafeVariable(PLACEHOLDER_USER, targetUser.asTag)
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
                                paginationPart.append(
                                    comment
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
                    paginationPart.append(
                        comment
                            .withVariable("page", countr)
                            .withVariable("pages", pages)
                    )
                    paginationPartList.add(paginationPart.toString())
                    paginationPart.clear()
                }

                sendPaginationMsgFetching(context, paginationPartList.first(), 0) { i ->
                    paginationPartList[i]
                }
            } else {
                sendRsp(context, msg)
            }
        }
    }

    class FindByCaseIdArg(parent: String) : AbstractCommand("$parent.findbycaseid") {

        init {
            name = "findByCaseId"
            aliases = arrayOf("find", "fbci")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val id = getStringFromArgsNMessage(context, 0, 6, 15) ?: return
            val dao = context.daoManager

            //timestamp, message
            val unorderedMap: MutableMap<Long, String> = hashMapOf()
            //put info inside maps

            val banMap = dao.banWrapper.getPunishMap(context, id)
            unorderedMap.putAll(banMap)

            if (unorderedMap.isEmpty()) {
                val kickMap = dao.kickWrapper.getPunishMap(context, id)
                unorderedMap.putAll(kickMap)
            }

            if (unorderedMap.isEmpty()) {
                val warnMap = dao.warnWrapper.getPunishMap(context, id)
                unorderedMap.putAll(warnMap)
            }

            if (unorderedMap.isEmpty()) {
                val muteMap = dao.muteWrapper.getPunishMap(context, id)
                unorderedMap.putAll(muteMap)
            }

            if (unorderedMap.isEmpty()) {
                val softbanMap = dao.softBanWrapper.getPunishMap(context, id)
                unorderedMap.putAll(softbanMap)
            }

            if (unorderedMap.isEmpty()) {
                sendRsp(context, "No punishment found for `$id`")
                return
            }
            //Collected all punishments
            val msg = unorderedMap.values.joinToString("")
            sendRspCodeBlock(context, msg, "INI")
        }
    }
}