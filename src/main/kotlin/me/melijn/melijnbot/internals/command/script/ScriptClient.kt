package me.melijn.melijnbot.internals.command.script

import kotlinx.coroutines.delay
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.administration.ScriptsCommand
import me.melijn.melijnbot.database.scripts.Script
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandClient
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.SPACE_REGEX
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.removePrefix
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ScriptClient(
    val container: Container,
    val commandMap: Map<String, AbstractCommand>
) {

    suspend fun runScript(
        event: MessageReceivedEvent,
        script: Script,
        triggerParts: List<String>,
        prefix: String?
    ) {
        var argBuilder = event.message.contentRaw
        if (prefix != null) {
            argBuilder = argBuilder.removePrefix(prefix, true).trimStart()
        }
        triggerParts.forEach {
            argBuilder = argBuilder.removePrefix(it, true).trimStart()
        }
        val rawArg = argBuilder
        val argLines = rawArg.split("\n")
        val args = rawArg.split(SPACE_REGEX)
        val lineArgMap: Map<Int, List<String>> =
            argLines.withIndex().map { (i, it) -> i to it.split(SPACE_REGEX) }.toMap()

        var missingArg = -1
        var missingLine = -1
        var missingLineArg = -1 to -1

        for ((index, cmdInfo) in script.commands.entries.sortedBy { it.key }) {
            val invoke = cmdInfo.first
            val filledArgs = mutableListOf<String>()
            for (it in cmdInfo.second) {
                when {
                    it.matches(ScriptsCommand.scriptArgRegex) -> {
                        val matchResult = ScriptsCommand.scriptArgRegex.find(it) ?: throw IllegalStateException()
                        val argIndex = (matchResult.groupValues[1].toIntOrNull() ?: throw IllegalStateException()) - 1
                        if (argIndex >= args.size) {
                            missingArg = argIndex
                            break
                        }
                        filledArgs.add(args[argIndex])
                    }
                    it.matches(ScriptsCommand.scriptLineRegex) -> {
                        val matchResult = ScriptsCommand.scriptLineRegex.find(it) ?: throw IllegalStateException()
                        val lineIndex = (matchResult.groupValues[1].toIntOrNull() ?: throw IllegalStateException()) - 1
                        if (lineIndex >= argLines.size) {
                            missingLine = lineIndex
                            break
                        }
                        filledArgs.add(argLines[lineIndex])
                    }
                    it.matches(ScriptsCommand.scriptLineArgRegex) -> {
                        val matchResult = ScriptsCommand.scriptLineArgRegex.find(it) ?: throw IllegalStateException()
                        val lineIndex = (matchResult.groupValues[1].toIntOrNull() ?: throw IllegalStateException()) - 1
                        val argIndex = (matchResult.groupValues[2].toIntOrNull() ?: throw IllegalStateException()) - 1
                        if (argIndex >= lineArgMap[lineIndex]?.size ?: 0) {
                            missingLineArg = lineIndex to argIndex
                            break
                        }
                        lineArgMap[lineIndex]?.get(argIndex)?.let { it1 -> filledArgs.add(it1) }
                    }
                    else -> filledArgs.add(it)
                }
            }
            if (missingArg != -1 || missingLine != -1 || missingLineArg.first != -1) {
                sendRsp(event.textChannel, container.daoManager, "Incorrect script args for `${script.trigger}`")
                return
            }

            val rootInvoke = invoke.takeWhile { it != ' ' }
            var command = commandMap[rootInvoke.toLowerCase()]
            val aliasesMap = mutableMapOf<String, List<String>>()
            val spaceMap = mutableMapOf<String, Int>()
            var searchedAliases = false
            val commandParts = invoke.split(" ").toMutableList()

            if (command == null) {
                val aliasCache = container.daoManager.aliasWrapper
                aliasesMap.putAll(aliasCache.getAliases(event.guild.idLong))
                searchedAliases = true
                // Find command by custom alias v
                for ((cmd, aliases) in aliasesMap) {
                    val id = cmd.toIntOrNull() ?: continue

                    for (alias in aliases) {
                        val aliasParts = alias.split(SPACE_REGEX)
                        if (aliasParts.size < commandParts.size) {
                            val matches = aliasParts.withIndex().all { commandParts[it.index + 1] == it.value }
                            if (!matches) continue

                            spaceMap["$id"] = aliasParts.size - 1
                            container.commandSet.firstOrNull {
                                it.id == id
                            }?.let {
                                command = it
                            }
                        }
                    }
                }
            }

            val finalCommand = command
            if (finalCommand == null) {
                sendRsp(event.textChannel, container.daoManager, "Unknown command")
                return
            }

            if (CommandClient.checksFailed(
                    container, finalCommand, finalCommand.id.toString(), event.message, false, commandParts
                )
            ) return

            commandParts.add(0, ">")
            val scriptCmd = CommandContext(
                event.message,
                commandParts,
                container,
                container.commandSet,
                spaceMap,
                aliasesMap,
                searchedAliases,
                contentRaw = ">" + invoke + filledArgs.joinToString("\" \"", " \"", "\"")

            )
            finalCommand.run(scriptCmd)
            delay(1000)
        }
    }

    fun eventIsForScript(
        triggerParts: List<String>,
        commandParts: List<String>,
        prefix: String?
    ): Boolean {
        val offset = if (prefix == null) 0 else 1
        return triggerParts.withIndex().all { (i, part) ->
            commandParts.getOrNull(i + offset)?.let { it.equals(part, true) } ?: false
        }
    }
}