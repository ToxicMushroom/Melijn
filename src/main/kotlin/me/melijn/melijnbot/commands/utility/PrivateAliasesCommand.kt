package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.commands.administration.ALIASES_CMD_LIMIT_PATH
import me.melijn.melijnbot.commands.administration.ALIASES_TOTAL_LIMIT_PATH
import me.melijn.melijnbot.commands.administration.AliasesCommand.Companion.getCommandPathInfo
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresGuildPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax

const val PREMIUM_TOTAL_ALIASES_LIMIT = 75
const val PREMIUM_CMD_ALIASES_LIMIT = 5

const val TOTAL_ALIASES_LIMIT = 20
const val CMD_ALIASES_LIMIT = 3

class PrivateAliasesCommand : AbstractCommand("command.privatealiases") {

    init {
        id = 178
        name = "privateAliases"
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            ClearArg(root),
            ClearAtArg(root),
            ListArg(root)
        )
        aliases = arrayOf("privateAlias", "pa")
        commandCategory = CommandCategory.UTILITY
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val aliasMap = context.daoManager.aliasWrapper.getAliases(context.authorId)
            if (aliasMap.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendRsp(context, msg)
                return
            }

            val sb = StringBuilder("```INI\n")
            var indexer = 1
            for ((cmdPath, aliases) in aliasMap.toSortedMap()) {
                val cmdId = cmdPath.split(".")[0].toInt()
                val rootCmd = context.commandList.firstOrNull { it.id == cmdId } ?: continue
                val idLessCmd = cmdPath.removePrefix("$cmdId")

                sb.append(indexer++).append(". [").append(rootCmd.name).append("]").append(idLessCmd.replace(".", " "))
                    .append("\n")
                for ((index, alias) in aliases.sorted().withIndex()) {
                    sb.append("    ").append(index + 1).append(": ")
                        .append(alias.escapeCodeblockMarkdown().escapeDiscordInvites())
                        .append("\n")
                }
            }
            sb.append("```")

            val listTitle = context.getTranslation("$root.title")
            sendRspCodeBlock(context, "$listTitle\n$sb", "INI", true)
        }
    }

    class ClearAtArg(parent: String) : AbstractCommand("$parent.clearat") {

        init {
            name = "clearAt"
            aliases = arrayOf("ca")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val aliasWrapper = context.daoManager.aliasWrapper
            val aliasMap = aliasWrapper.getAliases(context.authorId)
            val commandKeys = aliasMap
                .keys
                .sorted()

            val index = getIntegerFromArgNMessage(context, 0, 1, commandKeys.size) ?: return
            val cmdPath = commandKeys[index]
            val amount = aliasMap[cmdPath]?.size ?: 0

            aliasWrapper.clear(context.authorId, cmdPath)

            val cmdId = cmdPath.split(".")[0].toInt()
            val rootCmd = context.commandList.firstOrNull { it.id == cmdId }
            val idLessCmd = cmdPath.removePrefix("$cmdId")

            val msg = context.getTranslation("$root.cleared")
                .withVariable("amount", amount)
                .withVariable("cmd", (rootCmd?.name ?: "(deleted command)") + idLessCmd.replace(".", " "))
            sendRsp(context, msg)
        }
    }

    class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

        init {
            name = "clear"
            aliases = arrayOf("c")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val aliasWrapper = context.daoManager.aliasWrapper
            val aliasMap = aliasWrapper.getAliases(context.authorId)
            val removed = aliasMap[pathInfo.fullPath]?.size ?: 0

            aliasWrapper.clear(context.authorId, pathInfo.fullPath)

            val msg = context.getTranslation("$root.cleared")
                .withVariable("amount", removed)
                .withVariable("cmd", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendRsp(context, msg)
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "ra")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val aliasWrapper = context.daoManager.aliasWrapper
            val aliasMap = aliasWrapper.getAliases(context.authorId)
            val aliases = aliasMap[pathInfo.fullPath] ?: emptyList()
            if (aliases.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                    .withVariable("cmd", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)

                sendRsp(context, msg)
                return
            }

            val index = getIntegerFromArgNMessage(context, 1, 1, aliases.size) ?: return
            val alias = aliases[index - 1]

            aliasWrapper.remove(context.authorId, pathInfo.fullPath, alias)

            val msg = context.getTranslation("$root.removed")
                .withSafeVariable("alias", alias)
                .withVariable("cmd", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendRsp(context, msg)
        }

    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }
            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val alias = getStringFromArgsNMessage(
                context, 1, 1, 64, cantContainWords = arrayOf("%SPLIT%")
            ) ?: return

            context.daoManager.aliasWrapper.remove(context.authorId, pathInfo.fullPath, alias)

            val msg = context.getTranslation("$root.removed")
                .withSafeVariable("alias", alias)
                .withVariable("cmd", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendRsp(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            var total = 0
            val aliasWrapper = context.daoManager.aliasWrapper
            val aliases = aliasWrapper.getAliases(context.authorId)
            for ((_, aliasList) in aliases) {
                total += aliasList.size
            }

            if (total >= TOTAL_ALIASES_LIMIT && !isPremiumGuild(context)) {
                val replaceMap = mapOf(
                    Pair("limit", "$TOTAL_ALIASES_LIMIT"),
                    Pair("premiumLimit", "$PREMIUM_TOTAL_ALIASES_LIMIT")
                )

                sendFeatureRequiresGuildPremiumMessage(context, ALIASES_TOTAL_LIMIT_PATH, replaceMap)
                return
            } else if (total >= PREMIUM_TOTAL_ALIASES_LIMIT) {
                val msg = context.getTranslation("$root.limit.total")
                    .withVariable("limit", "$PREMIUM_TOTAL_ALIASES_LIMIT")
                sendRsp(context, msg)
                return
            }

            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val alias = getStringFromArgsNMessage(
                context, 1, 1, 64,
                cantContainWords = arrayOf("%SPLIT%")
            ) ?: return

            val cmdTotal = (aliases[pathInfo.fullPath] ?: emptyList()).size
            if (cmdTotal >= CMD_ALIASES_LIMIT && !isPremiumGuild(context)) {
                val replaceMap = mapOf(
                    Pair("limit", "$CMD_ALIASES_LIMIT"),
                    Pair("premiumLimit", "$PREMIUM_CMD_ALIASES_LIMIT")
                )

                sendFeatureRequiresGuildPremiumMessage(context, ALIASES_CMD_LIMIT_PATH, replaceMap)
                return
            } else if (cmdTotal >= PREMIUM_CMD_ALIASES_LIMIT) {
                val msg = context.getTranslation("$root.limit.cmd")
                    .withVariable("limit", "$PREMIUM_CMD_ALIASES_LIMIT")
                sendRsp(context, msg)
                return
            }

            aliasWrapper.add(context.authorId, pathInfo.fullPath, alias)

            val msg = context.getTranslation("$root.added")
                .withSafeVariable("alias", alias)
                .withVariable("cmd", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendRsp(context, msg)
        }
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}