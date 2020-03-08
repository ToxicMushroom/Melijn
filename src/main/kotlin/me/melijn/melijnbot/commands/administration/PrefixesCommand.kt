package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.utils.*

const val PREFIXES_LIMIT = 2
const val PREMIUM_PREFIXES_LIMIT = 10
const val PREFIXES_LIMIT_PATH = "premium.feature.prefix.limit"

class PrefixesCommand : AbstractCommand("command.prefixes") {

    init {
        id = 18
        name = "prefixes"
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(
            ListArg(root),
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root)
        )
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "view", "info")
        }

        override suspend fun execute(context: CommandContext) {
            val title = context.getTranslation("$root.response1.title")
            val prefixes = context.daoManager.guildPrefixWrapper.prefixCache.get(context.guildId).await()
                .sortedBy { it }

            var content = "```INI"
            for ((index, prefix) in prefixes.withIndex()) {
                content += "\n$index - [$prefix]"
            }
            content += "```"

            val msg = title + content
            sendMsg(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a", "put", "p")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.guildPrefixWrapper
            val ppList = wrapper.prefixCache[context.guildId].await()
            if (ppList.size >= PREFIXES_LIMIT && !isPremiumGuild(context)) {
                val replaceMap = mapOf(
                    Pair("limit", "$PREFIXES_LIMIT"),
                    Pair("premiumLimit", "$PREMIUM_PREFIXES_LIMIT")
                )

                sendFeatureRequiresGuildPremiumMessage(context, PREFIXES_LIMIT_PATH, replaceMap)
                return
            } else if (ppList.size >= PREMIUM_PREFIXES_LIMIT) {
                val msg = context.getTranslation("$root.limit")
                    .replace("%limit%", "$PREMIUM_PREFIXES_LIMIT")
                sendMsg(context, msg)
                return
            }

            val prefix = context.rawArg
            context.daoManager.guildPrefixWrapper.addPrefix(context.guildId, prefix)

            val msg = context.getTranslation("$root.response1")
                .replace(PREFIX_PLACE_HOLDER, prefix)
            sendMsg(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

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

            val wrapper = context.daoManager.guildPrefixWrapper
            val list = wrapper.prefixCache.get(context.guildId).await()
            val index = getIntegerFromArgNMessage(context, 0, 0, list.size - 1) ?: return

            val toRemove = list[index]
            wrapper.removePrefix(context.guildId, toRemove)

            val msg = context.getTranslation("$root.removed")
                .replace(PREFIX_PLACE_HOLDER, toRemove)
            sendMsg(context, msg)
        }
    }
}