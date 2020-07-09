package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class ManageHistoryCommand : AbstractCommand("command.managehistory") {

    init {
        id = 180
        name = "manageHistory"
        aliases = arrayOf("mh")
        children = arrayOf(
            RemoveArg(root),
            ClearArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

        init {
            name = "clear"
            aliases = arrayOf("cls", "c")
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

            val clearActive = getBooleanFromArgN(context, 2) ?: false

            val dao = context.daoManager
            var amount = 0
            for (type in types) {
                when (type) {
                    PunishmentType.BAN -> {
                        amount += dao.banWrapper.getBanMap(context, targetUser).size - (if (clearActive) 0 else 1)
                        dao.banWrapper.clear(context.guildId, targetUser.idLong, clearActive)
                    }
                    PunishmentType.KICK -> {
                        amount += dao.kickWrapper.getKickMap(context, targetUser).size
                        dao.kickWrapper.clear(context.guildId, targetUser.idLong)
                    }
                    PunishmentType.WARN -> {
                        amount += dao.warnWrapper.getWarnMap(context, targetUser).size
                        dao.warnWrapper.clear(context.guildId, targetUser.idLong)
                    }
                    PunishmentType.MUTE -> {
                        amount += dao.muteWrapper.getMuteMap(context, targetUser).size - (if (clearActive) 0 else 1)
                        dao.muteWrapper.clear(context.guildId, targetUser.idLong, clearActive)
                    }
                    PunishmentType.SOFTBAN -> {
                        amount += dao.softBanWrapper.getSoftBanMap(context, targetUser).size
                        dao.softBanWrapper.clear(context.guildId, targetUser.idLong)
                    }
                    else -> {

                    }
                }
            }

            val msg = if (amount > 0) {
                context.getTranslation("$root.cleared")
                    .withVariable("amount", "$amount")
                    .withVariable("type", context.args[0])
                    .withVariable(PLACEHOLDER_USER, targetUser.asTag)
            } else {
                context.getTranslation("$root.clearednone")
                    .withVariable("type", context.args[0])
                    .withVariable(PLACEHOLDER_USER, targetUser.asTag)
            }
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val targetUser = retrieveUserByArgsNMessage(context, 0) ?: return

            val caseId = getStringFromArgsNMessage(context, 1, 8, 14) ?: return

            val dao = context.daoManager
            var foundType: PunishmentType? = null

            loop@ for (type in PunishmentType.values()) {
                when (type) {
                    PunishmentType.BAN -> {
                        val ban = dao.banWrapper.getBans(context.guildId, targetUser.idLong)
                            .firstOrNull { it.banId == caseId } ?: continue@loop
                        dao.banWrapper.remove(ban)
                        foundType = type
                        break@loop
                    }
                    PunishmentType.KICK -> {
                        val kick = dao.kickWrapper.getKicks(context.guildId, targetUser.idLong)
                            .firstOrNull { it.kickId == caseId } ?: continue@loop
                        dao.kickWrapper.remove(kick)
                        foundType = type
                        break@loop
                    }
                    PunishmentType.WARN -> {
                        val warn = dao.warnWrapper.getWarns(context.guildId, targetUser.idLong)
                            .firstOrNull { it.warnId == caseId } ?: continue@loop
                        dao.warnWrapper.remove(warn)
                        foundType = type
                        break@loop
                    }
                    PunishmentType.MUTE -> {
                        val mute = dao.muteWrapper.getMutes(context.guildId, targetUser.idLong)
                            .firstOrNull { it.muteId == caseId } ?: continue@loop
                        dao.muteWrapper.remove(mute)
                        foundType = type
                        break@loop
                    }
                    PunishmentType.SOFTBAN -> {
                        val softBan = dao.softBanWrapper.getSoftBans(context.guildId, targetUser.idLong)
                            .firstOrNull { it.softBanId == caseId } ?: continue@loop
                        dao.softBanWrapper.remove(softBan)
                        foundType = type
                        break@loop
                    }
                    else -> {

                    }
                }
            }

            if (foundType == null) {
                val msg = context.getTranslation("$root.foundnothing")
                    .withVariable("caseId", caseId)
                    .withVariable(PLACEHOLDER_USER, targetUser.asTag)
                sendRsp(context, msg)
                return
            }

            val msg = context.getTranslation("$root.removed")
                .withVariable("caseId", caseId)
                .withVariable("type", foundType.toUCC())
                .withVariable(PLACEHOLDER_USER, targetUser.asTag)
            sendRsp(context, msg)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}