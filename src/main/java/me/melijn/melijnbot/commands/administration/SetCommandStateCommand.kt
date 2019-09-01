package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.enums.CommandState
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.Translateable
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
        sendSyntax(context, syntax)
    }

    class GlobalArg(parentRoot: String) : AbstractCommand("$parentRoot.global") {

        init {
            name = "global"
            aliases = arrayOf("g")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context, syntax)
                return
            }

            val commands = getCommandsFromArgNMessage(context, 0) ?: return
            val commandState = enumValueOrNull<CommandState>(context.args[1])
            if (commandState == null) {
                sendMsg(context, Translateable("message.unknown.commandstate").string(context)
                        .replace(PLACEHOLDER_ARG, context.args[1]))
                return
            }

            val dao = context.daoManager.disabledCommandWrapper
            dao.setCommandState(context.getGuildId(), commands, commandState)

            val msg = Translateable("$root.response1").string(context)
                    .replace("%s%", if (commands.size > 1) "s" else "")
                    .replace("%es%", if (commands.size > 1) "" else "es")
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
                sendSyntax(context, syntax)
                return
            }

            val channel = getTextChannelByArgsNMessage(context, 0) ?: return
            val commands = getCommandsFromArgNMessage(context, 1) ?: return
            val commandState = enumValueOrNull<ChannelCommandState>(context.args[2])
            if (commandState == null) {
                sendMsg(context, Translateable("message.unknown.channelcommandstate").string(context)
                        .replace(PLACEHOLDER_ARG, context.args[2]))
                return
            }

            val dao = context.daoManager.channelCommandStateWrapper
            dao.setCommandState(context.getGuildId(), channel.idLong, commands, commandState)

            val msg = Translateable("$root.response1").string(context)
                    .replace("%s%", if (commands.isNotEmpty()) "s" else "")
                    .replace("%channel%", "#${channel.name}")
                    .replace("%commandCount%", commands.size.toString())
                    .replace("%es%", if (commands.size > 1) "" else "es")
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
            if (context.args.isEmpty()) {
                val ids = context.daoManager.disabledCommandWrapper.disabledCommandsCache.get(context.getGuildId()).get()
                val commands = context.getCommands().filter { cmd -> ids.contains(cmd.id) }
                val title = Translateable("$root.globaldisabled.response1").string(context)
                var content = "```INI"
                for ((index, cmd) in commands.withIndex()) {
                    content += "\n$index - [${cmd.name}]"
                }
                content+= "```"
                val msg = title + content
                sendMsg(context, msg)
            } else {
                val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                val stateMap = context.daoManager.channelCommandStateWrapper.channelCommandsStateCache.get(channel.idLong).get()
                val commands = context.getCommands().filter { cmd -> stateMap.keys.contains(cmd.id) }
                val title = Translateable("$root.channelstate.response1").string(context)
                        .replace("%channel%", "#${channel.name}")
                var content = "```INI"
                for ((index, cmd) in commands.withIndex()) {
                    content += "\n$index - [${cmd.name}] - ${stateMap[cmd.id]}"
                }
                content+= "```"
                val msg = title + content
                sendMsg(context, msg)
            }
        }
    }
}

