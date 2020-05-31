package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.utils.*

const val UNKNOWN_COMMAND = "message.unknown.command"
const val TOTAL_ALIASES_LIMIT = 5
const val CMD_ALIASES_LIMIT = 2
const val PREMIUM_TOTAL_ALIASES_LIMIT = 50
const val PREMIUM_CMD_ALIASES_LIMIT = 10
const val ALIASES_TOTAL_LIMIT_PATH = "premium.feature.aliases.total.limit"
const val ALIASES_CMD_LIMIT_PATH = "premium.feature.aliases.cmd.limit"


class AliasesCommand : AbstractCommand("command.aliases") {

    init {
        id = 177
        name = "aliases"
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            ClearArg(root),
            ClearAtArg(root),
            ListArg(root)
        )
        aliases = arrayOf("alias")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val aliasMap = context.daoManager.aliasWrapper.aliasCache.get(context.guildId).await()
            if (aliasMap.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendMsg(context, msg)
                return
            }

            val sb = StringBuilder()
            var indexer = 1
            for ((cmdPath, aliases) in aliasMap.toSortedMap()) {
                val cmdId = cmdPath.split(".")[0].toInt()
                val rootCmd = context.commandList.first { it.id == cmdId }
                val idLessCmd = cmdPath.removePrefix("$cmdId")

                sb.append(indexer++).append(". ").append(rootCmd.name).append(idLessCmd.replace(".", " "))
                    .append("\n")
                for ((index, alias) in aliases.sorted().withIndex()) {
                    sb.append("    ").append(index).append(". ").append(alias).append("\n")
                }
            }

            val listTitle = context.getTranslation("$root.title")
            sendMsg(context, "$listTitle\n${sb.toString()}")
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

            val aliasMap = context.daoManager.aliasWrapper.aliasCache.get(context.guildId).await()
            val commandKeys = aliasMap
                .keys
                .sorted()

            val index = getIntegerFromArgNMessage(context, 0, 1, commandKeys.size) ?: return
            val cmdPath = commandKeys[index]
            val amount = aliasMap[cmdPath]?.size ?: 0

            context.daoManager.aliasWrapper.clear(context.guildId, cmdPath)

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
            val aliasMap = context.daoManager.aliasWrapper.aliasCache.get(context.guildId).await()
            val removed = aliasMap[pathInfo.fullPath]?.size ?: 0

            context.daoManager.aliasWrapper.clear(context.guildId, pathInfo.fullPath)

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
            val aliasMap = context.daoManager.aliasWrapper.aliasCache.get(context.guildId).await()
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

            context.daoManager.aliasWrapper.remove(context.guildId, pathInfo.fullPath, alias)

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

            context.daoManager.aliasWrapper.remove(context.guildId, pathInfo.fullPath, alias)

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
            val aliases = context.daoManager.aliasWrapper.aliasCache.get(context.guildId).await()
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
                val msg = context.getTranslation("$root.limit")
                    .replace("%limit%", "$PREMIUM_TOTAL_ALIASES_LIMIT")
                sendMsg(context, msg)
                return
            }

            val pathInfo = getCommandPathInfo(context, 0) ?: return
            val alias = getStringFromArgsNMessage(context, 1, 1, 64,
                cantContainChars = arrayOf(' '), cantContainWords = arrayOf("%SPLIT%")) ?: return

            val cmdTotal = (aliases[pathInfo.fullPath] ?: emptyList()).size
            if (cmdTotal >= CMD_ALIASES_LIMIT && !isPremiumGuild(context)) {
                val replaceMap = mapOf(
                    Pair("limit", "$CMD_ALIASES_LIMIT"),
                    Pair("premiumLimit", "$PREMIUM_CMD_ALIASES_LIMIT")
                )

                sendFeatureRequiresGuildPremiumMessage(context, ALIASES_CMD_LIMIT_PATH, replaceMap)
                return
            } else if (cmdTotal >= PREMIUM_CMD_ALIASES_LIMIT) {
                val msg = context.getTranslation("$root.limit")
                    .replace("%limit%", "$PREMIUM_CMD_ALIASES_LIMIT")
                sendMsg(context, msg)
                return
            }

            context.daoManager.aliasWrapper.add(context.guildId, pathInfo.fullPath, alias)

            val msg = context.getTranslation("$root.added")
                .replace("%alias%", alias)
                .replace("%cmd%", pathInfo.rootCmd.name + pathInfo.idLessCmd.replace(".", " "))
            sendMsg(context, msg)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    companion object {
        fun find(children: Array<AbstractCommand>, parts: List<String>, progress: Int): AbstractCommand? {
            for (child in children) {
                if (!child.isCommandFor(parts[progress])) continue
                return if (parts.size <= progress) {
                    child
                } else {
                    find(child.children, parts, progress + 1)
                }
            }
            return null
        }

        suspend fun getCommandPathInfo(context: CommandContext, index: Int): CmdPathInfo? {
            val commandParts = context.args[index].split("\\s+".toRegex())
            val cmd = find(context.commandList.toTypedArray(), commandParts, 0)
            if (cmd == null) {
                val msg = context.getTranslation(UNKNOWN_COMMAND)
                sendMsg(context, msg)
                return null
            }

            val rootCmd = context.commandList.first { it.isCommandFor(commandParts[0]) }
            val idLessCmd = cmd.root.removePrefix(rootCmd.root)
            return CmdPathInfo(idLessCmd, rootCmd)
        }
    }

    data class CmdPathInfo(
        val idLessCmd: String,
        val rootCmd: AbstractCommand,
        val fullPath: String = "${rootCmd.id}$idLessCmd"
    )
}