package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.i18n
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

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }
        val types = PunishmentType.getMatchingTypesFromNode(context.args[0])
        if (types.isEmpty()) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "message.unknown.punishmenttype")
                .replace(PLACEHOLDER_ARG, context.args[0])
            sendMsg(context, msg)
            return
        }

        val targetUser = retrieveUserByArgsNMessage(context, 1) ?: return
        val unorderedMap: MutableMap<Long, String> = hashMapOf()
        val shardManager = context.getShardManager() ?: return
        val dao = context.daoManager
        val guildId = context.getGuildId()

        //put info inside maps
        for (type in types) {
            when (type) {
                PunishmentType.BAN -> {
                    val banMap = dao.banWrapper.getBanMap(shardManager, guildId, targetUser)
                    unorderedMap.putAll(banMap)
                }
                PunishmentType.KICK -> {
                    val kickMap = dao.kickWrapper.getKickMap(shardManager, guildId, targetUser)
                    unorderedMap.putAll(kickMap)
                }
                PunishmentType.WARN -> {
                    val warnMap = dao.warnWrapper.getWarnMap(shardManager, guildId, targetUser)
                    unorderedMap.putAll(warnMap)
                }
                PunishmentType.MUTE -> {
                    val muteMap = dao.muteWrapper.getMuteMap(shardManager, guildId, targetUser)
                    unorderedMap.putAll(muteMap)
                }
            }
        }

        val orderedMap = unorderedMap.toSortedMap().toMap()

        //Collected all punishments
        val msg = orderedMap.values.joinToString("")

        if (msg.isBlank()) {
            val language = context.getLanguage()
            val or = i18n.getTranslation(language, "or")
            var readableList = types.subList(0, types.size - 1).joinToString(", ", transform = { type ->
                type.name.toLowerCase()
            })
            if (readableList.isBlank()) {
                readableList = types.last().name.toLowerCase()
            } else {
                readableList += " $or " + types.last().name.toLowerCase()
            }


            val noHistory = i18n.getTranslation(language, "$root.nohistory")
                .replace(PLACEHOLDER_USER, targetUser.asTag)
                .replace("%typeList%", readableList)
            sendMsg(context, noHistory)
        } else {
            sendMsgCodeBlocks(context, msg, "INI")
        }
    }
}