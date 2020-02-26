package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.*
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class PrivatePrefixesCommand : AbstractCommand("command.privateprefixes") {
    init {
        id = 19
        name = "privatePrefixes"
        aliases = arrayOf("pp")
        children = arrayOf(
            ListArg(root),
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root)
        )
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val title = context.getTranslation("$root.response1.title")

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

    class AddArg(root: String) : AbstractCommand("$root.add") {

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

            val msg = context.getTranslation("$root.response1")
                .replace(PREFIX_PLACE_HOLDER, prefix)
            sendMsg(context, msg)
        }
    }

    class RemoveArg(root: String) : AbstractCommand("$root.remove") {

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

            val msg = context.getTranslation("$root.response1")
                .replace(PREFIX_PLACE_HOLDER, prefix)
            sendMsg(context, msg)
        }
    }


    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "deleteAt", "dat")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.userPrefixWrapper
            val list = wrapper.prefixCache.get(context.authorId).await()
            val index = getIntegerFromArgNMessage(context, 0, 0, list.size - 1) ?: return

            val toRemove = list[index]
            wrapper.removePrefix(context.authorId, toRemove)

            val msg = context.getTranslation("$root.removed")
                .replace(PREFIX_PLACE_HOLDER, toRemove)
            sendMsg(context, msg)
        }
    }
}