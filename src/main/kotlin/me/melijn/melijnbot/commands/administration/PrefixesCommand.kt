package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class PrefixesCommand : AbstractCommand("command.prefixes") {

    init {
        id = 18
        name = "prefixes"
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(
            ListCommand(root),
            AddCommand(root),
            RemoveCommand(root)
        )
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class ListCommand(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "view", "info")
        }

        override suspend fun execute(context: CommandContext) {
            val title = context.getTranslation("$root.response1.title")
            val prefixes = context.daoManager.guildPrefixWrapper.prefixCache.get(context.guildId).await()

            var content = "```INI"
            for ((index, prefix) in prefixes.withIndex()) {
                content += "\n$index - [$prefix]"
            }
            content += "```"

            val msg = title + content
            sendMsg(context, msg)
        }
    }

    class AddCommand(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a", "put", "p")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val prefix = context.rawArg
            context.daoManager.guildPrefixWrapper.addPrefix(context.guildId, prefix)

            val msg = context.getTranslation("$root.response1")
                .replace("%prefix%", prefix)
            sendMsg(context, msg)
        }
    }

    class RemoveCommand(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r", "delete", "d")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val prefix = context.rawArg
            context.daoManager.guildPrefixWrapper.removePrefix(context.guildId, prefix)

            val msg = context.getTranslation("$root.response1")
                .replace("%prefix%", prefix)
            sendMsg(context, msg)
        }
    }
}