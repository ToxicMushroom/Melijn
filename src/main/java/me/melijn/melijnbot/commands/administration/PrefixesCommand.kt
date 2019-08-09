package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class PrefixesCommand : AbstractCommand("command.prefixes") {

    init {
        id = 18
        name = "prefixes"
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(ViewCommand(root), AddCommand(root), RemoveCommand(root))
    }

    override fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class ViewCommand(root: String) : AbstractCommand("$root.view") {

        init {
            name = "view"
            aliases = arrayOf("v", "vw", "list")
        }

        override fun execute(context: CommandContext) {
            val title = Translateable("$root.response1.title").string(context)
            var content = "```INI"
            val prefixes = context.daoManager.guildPrefixWrapper.prefixCache.get(context.guildId).get()
            for ((index, prefix) in prefixes.withIndex()) {
                content += "\n$index - [$prefix]"
            }
            content += "```"
            val msg = title + content
            sendMsg(context, msg)
        }
    }

    class AddCommand(root: String) : AbstractCommand("$root.add") {

        init {
            name = "add"
            aliases = arrayOf("a", "put", "p")
        }

        override fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context, syntax)
                return
            }

            val prefix = context.rawArg
            context.daoManager.guildPrefixWrapper.addPrefix(context.guildId, prefix)
            val msg = Translateable("$root.response1").string(context)
                    .replace("%prefix%", prefix)
            sendMsg(context, msg)
        }
    }

    class RemoveCommand(root: String) : AbstractCommand("$root.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r", "delete", "d")
        }

        override fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context, syntax)
                return
            }

            val prefix = context.rawArg
            context.daoManager.guildPrefixWrapper.removePrefix(context.guildId, prefix)
            val msg = Translateable("$root.response1").string(context)
                    .replace("%prefix%", prefix)
            sendMsg(context, msg)
        }
    }
}