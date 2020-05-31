package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commands.administration.AliasesCommand.Companion.getCommandPathInfo
import me.melijn.melijnbot.objects.command.*
import me.melijn.melijnbot.objects.utils.*

const val TOTAL_ALIASES_LIMIT = 25
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
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        aliases = arrayOf("privateAlias", "pa")
        commandCategory = CommandCategory.UTILITY
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val aliasMap = context.daoManager.aliasWrapper.aliasCache.get(context.authorId).await()
            if (aliasMap.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendMsg(context, msg)
                return
            }

            val sb = StringBuilder("```INI\n")
            var indexer = 1
            for ((cmdPath, aliases) in aliasMap.toSortedMap()) {
                val cmdId = cmdPath.split(".")[0].toInt()
                val rootCmd = context.commandList.first { it.id == cmdId }
                val idLessCmd = cmdPath.removePrefix("$cmdId")

                sb.append(indexer++).append(". [").append(rootCmd.name).append("]").append(idLessCmd.replace(".", " "))
                    .append("\n")
                for ((index, alias) in aliases.sorted().withIndex()) {
                    sb.append("    ").append(index + 1).append(": ").append(alias).append("\n")
                }
            }
            sb.append("```")

            val listTitle = context.getTranslation("$root.title")
            sendMsgCodeBlock(context, "$listTitle\n$sb", "INI", true)
        }
    }

    class ClearAtArg(parent: String) : AbstractCommand("$parent.clearat") {

        init {
            name = "clearAt"
            aliases = arrayOf("ca")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val aliasWrapper = context.daoManager.aliasWrapper
            val aliasMap = aliasWrapper.aliasCache.get(context.authorId).await()
            val commandKeys = aliasMap
                .keys
                .sorted()

            val index = getIntegerFromArgNMessage(context, 0, 1, commandKeys.size) ?: return
            val cmdPath = commandKeys[index]
            val amount = aliasMap[cmdPath]?.size ?: 0

            aliasWrapper.clear(context.authorId, cmdPath)

            val cmdId = cmdPath.split(".")[0].toInt()
            val rootCmd = context.commandList.first { it.id == cmdId }
            val idLessCmd = cmdPath.removePrefix("$cmdId")

            val msg = context.getTranslation("$root.cleared")
                .replace("%amount%", amount)
                .replace("%cmd%", rootCmd.name + idLessCmd.replace(".", " "))
            sendMsg(context, msg)
        }
    }

    class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

        init {
            name = "clear"
            aliases = arrayOf("c")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val aliasWrapper = context.daoManager.aliasWrapper
            val aliasMap = aliasWrapper.aliasCache.get(context.authorId).await()
            val removed = aliasMap[pathInfo.fullPath]?.size ?: 0

            aliasWrapper.clear(context.authorId, pathInfo.fullPath)

            val msg = context.getTranslation("$root.cleared")
                .replace("%amount%", removed)
                .replace("%cmd%", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendMsg(context, msg)
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "ra")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val aliasWrapper = context.daoManager.aliasWrapper
            val aliasMap = aliasWrapper.aliasCache.get(context.authorId).await()
            val aliases = aliasMap[pathInfo.fullPath] ?: emptyList()
            if (aliases.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                    .replace("%cmd%", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
                    .replace(PLACEHOLDER_PREFIX, context.usedPrefix)

                sendMsg(context, msg)
                return
            }

            val index = getIntegerFromArgNMessage(context, 1, 1, aliases.size) ?: return
            val alias = aliases[index]

            aliasWrapper.remove(context.authorId, pathInfo.fullPath, alias)

            val msg = context.getTranslation("$root.removed")
                .replace("%alias%", alias)
                .replace("%cmd%", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendMsg(context, msg)
        }

    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r")
        }

        override suspend fun execute(context: CommandContext) {
            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val alias = getStringFromArgsNMessage(context, 1, 1, 64,
                cantContainChars = arrayOf(' '), cantContainWords = arrayOf("%SPLIT%")) ?: return

            context.daoManager.aliasWrapper.remove(context.authorId, pathInfo.fullPath, alias)

            val msg = context.getTranslation("$root.removed")
                .replace("%alias%", alias)
                .replace("%cmd%", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendMsg(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            var total = 0
            val aliasWrapper = context.daoManager.aliasWrapper
            val aliases = aliasWrapper.aliasCache.get(context.guildId).await()
            for ((_, aliasList) in aliases) {
                total += aliasList.size
            }

            if (total >= TOTAL_ALIASES_LIMIT) {
                val msg = context.getTranslation("$root.limit.total")
                    .replace("%limit%", "$TOTAL_ALIASES_LIMIT")

                sendMsg(context, msg)
                return
            }

            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val alias = getStringFromArgsNMessage(context, 1, 1, 64,
                cantContainChars = arrayOf(' '), cantContainWords = arrayOf("%SPLIT%")) ?: return

            val cmdTotal = (aliases[pathInfo.fullPath] ?: emptyList()).size
            if (cmdTotal >= CMD_ALIASES_LIMIT) {
                val msg = context.getTranslation("$root.limit.cmd")
                    .replace("%limit%", "$CMD_ALIASES_LIMIT")

                sendMsg(context, msg)
                return
            }

            aliasWrapper.add(context.guildId, pathInfo.fullPath, alias)

            val msg = context.getTranslation("$root.added")
                .replace("%alias%", alias)
                .replace("%cmd%", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendMsg(context, msg)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}