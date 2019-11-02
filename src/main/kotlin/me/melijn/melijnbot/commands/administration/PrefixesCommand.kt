package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class PrefixesCommand : AbstractCommand("command.prefixes") {

    init {
        id = 18
        name = "prefixes"
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(ViewCommand(root), AddCommand(root), RemoveCommand(root))
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
            val prefixes = context.daoManager.guildPrefixWrapper.prefixCache.get(context.getGuildId()).await()

            var content = "```INI"
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
            context.daoManager.guildPrefixWrapper.addPrefix(context.getGuildId(), prefix)

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
            context.daoManager.guildPrefixWrapper.removePrefix(context.getGuildId(), prefix)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.response1")
                .replace("%prefix%", prefix)
            sendMsg(context, msg)
        }
    }
}