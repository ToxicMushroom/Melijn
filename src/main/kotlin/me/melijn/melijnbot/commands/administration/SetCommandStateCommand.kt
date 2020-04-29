package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.enums.CommandState
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_CHANNELCOMMANDSTATE
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_COMMANDSTATE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*

class SetCommandStateCommand : AbstractCommand("command.setcommandstate") {

    init {
        id = 16
        name = "setCommandState"
        aliases = arrayOf("scs")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(GlobalArg(root), ChannelArg(root), InfoArg(root))
    }

    //setCommandState <global, channel, info>
    //setCommandState global commandNode* state*
    //setCommandState channel channel* commandNode* state*
    //setCommandState info [channel*]

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class GlobalArg(parentRoot: String) : AbstractCommand("$parentRoot.global") {

        init {
            name = "global"
            aliases = arrayOf("g")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val commands = getCommandIdsFromArgNMessage(context, 0) ?: return
            val commandState: CommandState = getEnumFromArgNMessage(context, 1, MESSAGE_UNKNOWN_COMMANDSTATE) ?: return

            val dao = context.daoManager.disabledCommandWrapper
            dao.setCommandState(context.guildId, commands, commandState)
            val path = "$root.response1" + if (commands.size > 1) {
                ".multiple"
            } else {
                ""
            }
            val msg = context.getTranslation(path)
                .replace("%commandCount%", commands.size.toString())
                .replace("%state%", commandState.toString())
                .replace("%commandNode%", context.args[0])

            sendMsg(context, msg)
        }
    }

    class ChannelArg(parentRoot: String) : AbstractCommand("$parentRoot.channel") {

        init {
            name = "channel"
            aliases = arrayOf("c")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 3) {
                sendSyntax(context)
                return
            }

            val channel = getTextChannelByArgsNMessage(context, 0) ?: return
            val commands = getCommandIdsFromArgNMessage(context, 1) ?: return

            val nullS = context.args[2] == "null"
            val commandState = if (nullS) {
                ChannelCommandState.DEFAULT
            } else {
                getEnumFromArgNMessage(context, 2, MESSAGE_UNKNOWN_CHANNELCOMMANDSTATE) ?: return
            }

            val dao = context.daoManager.channelCommandStateWrapper
            dao.setCommandState(context.guildId, channel.idLong, commands, commandState)

            val path = "$root.response1" + if (commands.size > 1) {
                ".multiple"
            } else {
                ""
            }

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, path)
                .replace(PLACEHOLDER_CHANNEL, channel.asTag)
                .replace("%commandCount%", commands.size.toString())
                .replace("%state%", commandState.toString())
                .replace("%commandNode%", context.args[1])

            sendMsg(context, msg)
        }
    }

    class InfoArg(parentRoot: String) : AbstractCommand("$parentRoot.info") {

        init {
            name = "info"
            aliases = arrayOf("i")
        }

        override suspend fun execute(context: CommandContext) {
            val daoManager = context.daoManager
            if (context.args.isEmpty()) {
                val ids = daoManager.disabledCommandWrapper.disabledCommandsCache.get(context.guildId).await()
                val commandNames = mutableListOf<String>()
                val filteredCommands = context.commandList
                    .filter { cmd -> ids.contains(cmd.id.toString()) }
                    .map { cmd -> cmd.name }
                    .toList()

                val filteredCCs = daoManager.customCommandWrapper.customCommandCache.get(context.guildId).await()
                    .filter { (ccId) -> ids.contains("cc.$ccId") }
                    .map { cmd -> cmd.name }
                    .toList()

                commandNames.addAll(filteredCommands)
                commandNames.addAll(filteredCCs)


                val language = context.getLanguage()
                val title = i18n.getTranslation(language, "$root.globaldisabled.response1")

                var content = "```INI"
                for ((index, name) in commandNames.withIndex()) {
                    content += "\n$index - [${name}]"
                }
                content += "```"

                val msg = title + content
                sendMsgCodeBlock(context, msg, "INI")
            } else {
                val channel = getTextChannelByArgsNMessage(context, 0) ?: return


                val stateMap = daoManager.channelCommandStateWrapper.channelCommandsStateCache.get(channel.idLong).await()
                val ids = stateMap.keys
                val commandMap = HashMap<String, String>()
                val filteredCommands = context.commandList
                    .filter { cmd -> ids.contains(cmd.id.toString()) }
                    .map { cmd -> cmd.id.toString() to cmd.name }
                    .toMap()

                val filteredCCs = daoManager.customCommandWrapper.customCommandCache.get(context.guildId).await()
                    .filter { (ccId) -> ids.contains("cc.$ccId") }
                    .map { (ccId, ccName) -> ("cc.$ccId") to ccName }
                    .toMap()

                commandMap.putAll(filteredCommands)
                commandMap.putAll(filteredCCs)

                val title = context.getTranslation("$root.channelstate.response1")
                    .replace(PLACEHOLDER_CHANNEL, channel.asTag)

                var content = "```INI"
                for ((index, entry) in commandMap.entries.withIndex()) {
                    content += "\n$index - [${entry.value}] - ${stateMap[entry.key]}"
                }
                content += "```"

                val msg = title + content
                sendMsgCodeBlock(context, msg, "INI")
            }
        }
    }
}

