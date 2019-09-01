package me.melijn.melijnbot.commands.supporter

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class PrivatePrefixesCommand : AbstractCommand("command.privateprefixes") {
    init {
        id = 19
        name = "privatePrefixes"
        aliases = arrayOf("pp")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(ViewCommand(root), AddCommand(root), RemoveCommand(root))
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class ViewCommand(root: String) : AbstractCommand("$root.view") {

        init {
            name = "view"
            aliases = arrayOf("v", "vw", "list")
        }

        override suspend fun execute(context: CommandContext) {
            val title = Translateable("$root.response1.title").string(context)
            var content = "```INI"
            val prefixes = context.daoManager.userPrefixWrapper.prefixCache.get(context.authorId).get()
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

        override suspend fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context, syntax)
                return
            }

            val prefix = context.rawArg
            context.daoManager.userPrefixWrapper.addPrefix(context.authorId, prefix)
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

        override suspend fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context, syntax)
                return
            }

            val prefix = context.rawArg
            context.daoManager.userPrefixWrapper.removePrefix(context.authorId, prefix)
            val msg = Translateable("$root.response1").string(context)
                    .replace("%prefix%", prefix)
            sendMsg(context, msg)
        }
    }
}