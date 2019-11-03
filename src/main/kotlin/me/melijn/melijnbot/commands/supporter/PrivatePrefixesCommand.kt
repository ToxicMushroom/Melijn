package me.melijn.melijnbot.commands.supporter

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class PrivatePrefixesCommand : AbstractCommand("command.privateprefixes") {
    init {
        id = 19
        name = "privatePrefixes"
        aliases = arrayOf("pp")
        children = arrayOf(ViewCommand(root), AddCommand(root), RemoveCommand(root))
        commandCategory = CommandCategory.SUPPORTER
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class ViewCommand(root: String) : AbstractCommand("$root.view") {

        init {
            name = "view"
            aliases = arrayOf("v", "vw", "list")
        }

        override suspend fun execute(context: CommandContext) {
            val language = context.getLanguage()
            val title = i18n.getTranslation(language, "$root.response1.title")

            var content = "```INI"
            val prefixes = context.daoManager.userPrefixWrapper.prefixCache.get(context.authorId).await()
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
                sendSyntax(context)
                return
            }

            val prefix = context.rawArg
            context.daoManager.userPrefixWrapper.addPrefix(context.authorId, prefix)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.response1")
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
                sendSyntax(context)
                return
            }

            val prefix = context.rawArg
            context.daoManager.userPrefixWrapper.removePrefix(context.authorId, prefix)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.response1")
                .replace("%prefix%", prefix)
            sendMsg(context, msg)
        }
    }
}