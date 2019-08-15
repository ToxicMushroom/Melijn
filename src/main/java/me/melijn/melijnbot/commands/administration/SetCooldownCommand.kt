package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.*

class SetCooldownCommand : AbstractCommand("command.setcooldown") {

    init {
        id = 17
        name = "setCooldown"
        aliases = arrayOf("sc")
        children = arrayOf(ChannelArg(root), GlobalArg(root), InfoArg(root))
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class ChannelArg(parentRoot: String) : AbstractCommand("$parentRoot.channel") {

        init {
            name = "channel"
            aliases = arrayOf("c")
        }

        override fun execute(context: CommandContext) {
            if (context.args.size < 3) {
                sendSyntax(context, syntax)
                return
            }
            val channel = getTextChannelByArgsNMessage(context, 0) ?: return
            val commands = getCommandsFromArgNMessage(context, 1) ?: return
            val cooldown = getLongFromArgNMessage(context, 2) ?: return

            val daoWrapper = context.daoManager.commandChannelCoolDownWrapper
            daoWrapper.setCooldowns(channel.guild.idLong, channel.idLong, commands, cooldown)

            val msg = Translateable("$root.response1").string(context)
                    .replace("%channel%", "#${channel.name}")
                    .replace("%commandCount%", commands.size.toString())
                    .replace("%commandNode%", context.args[1])
                    .replace("%s%", if (commands.size > 1) "s" else "")
                    .replace("%cooldown%", cooldown.toString())
            sendMsg(context, msg)
        }
    }

    class GlobalArg(parentRoot: String) : AbstractCommand("$parentRoot.global") {

        init {
            name = "global"
            aliases = arrayOf("g")
        }

        override fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context, syntax)
                return
            }
            val commands = getCommandsFromArgNMessage(context, 0) ?: return
            val cooldown = getLongFromArgNMessage(context, 1) ?: return

            val daoWrapper = context.daoManager.commandCooldownWrapper
            daoWrapper.setCooldowns(context.getGuildId(), commands, cooldown)

            val msg = Translateable("$root.response1").string(context)
                    .replace("%commandCount%", commands.size.toString())
                    .replace("%commandNode%", context.args[1])
                    .replace("%cooldown%", cooldown.toString())
                    .replace("%s%", if (commands.size > 1) "s" else "")

            sendMsg(context, msg)
        }
    }

    class InfoArg(parentRoot: String) : AbstractCommand("$parentRoot.info") {

        init {
            name = "info"
            aliases = arrayOf("i", "list", "ls", "l")
        }

        override fun execute(context: CommandContext) {
            val map: Map<Int, Long>
            val title: String
            if (context.args.isNotEmpty()) {
                val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                map = context.daoManager.commandChannelCoolDownWrapper.commandChannelCooldownCache.get(channel.idLong).get()
                title = Translateable("$root.response1.title").string(context)
                        .replace("%channel%", "#${channel.name}")
            } else {
                map = context.daoManager.commandCooldownWrapper.commandCooldownCache.get(context.getGuildId()).get()
                title = Translateable("$root.response2.title").string(context)
            }

            var content = "```INI"

            for ((index, entry) in map.entries.withIndex()) {
                val cmd = context.getCommands().firstOrNull { cmd -> cmd.id == entry.key }
                if (cmd != null)
                    content += "$index - [${cmd.name}]"
            }
            content += "```"

            val msg = title + content
            sendMsgCodeBlock(context, msg, "INI")
        }
    }
}