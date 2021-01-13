package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.isPremiumGuild
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresGuildPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

const val PREFIXES_LIMIT = 3
const val PREMIUM_PREFIXES_LIMIT = 10
const val PREFIXES_LIMIT_PATH = "premium.feature.prefix.limit"

class PrefixesCommand : AbstractCommand("command.prefixes") {

    init {
        id = 18
        name = "prefixes"
        aliases = arrayOf("prefix")
        children = arrayOf(
            ListArg(root),
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "view", "info")
        }

        override suspend fun execute(context: ICommandContext) {
            val title = context.getTranslation("$root.response1.title")
            val prefixes = context.daoManager.guildPrefixWrapper.getPrefixes(context.guildId)
                .sortedBy { it }

            val defPrefixMsg = context.getTranslation("$root.defprefix")
                .withVariable(PLACEHOLDER_PREFIX, context.prefix)

            var content = "```INI"
            if (prefixes.isEmpty()) content += "\n$defPrefixMsg"
            for ((index, prefix) in prefixes.withIndex()) {
                content += "\n$index - [$prefix]"
            }
            content += "```"

            val msg = title + content
            sendRsp(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a", "put", "p")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.guildPrefixWrapper
            val ppList = wrapper.getPrefixes(context.guildId)
            if (ppList.size >= PREFIXES_LIMIT && !isPremiumGuild(context)) {
                val replaceMap = mapOf(
                    Pair("limit", "$PREFIXES_LIMIT"),
                    Pair("premiumLimit", "$PREMIUM_PREFIXES_LIMIT")
                )

                sendFeatureRequiresGuildPremiumMessage(context, PREFIXES_LIMIT_PATH, replaceMap)
                return
            } else if (ppList.size >= PREMIUM_PREFIXES_LIMIT) {
                val msg = context.getTranslation("$root.limit")
                    .withVariable("limit", "$PREMIUM_PREFIXES_LIMIT")
                sendRsp(context, msg)
                return
            }

            val prefix = context.rawArg.take((1024 - (PREFIXES_LIMIT * "%SPLIT%".length)) / PREMIUM_PREFIXES_LIMIT)
            context.daoManager.guildPrefixWrapper.addPrefix(context.guildId, prefix)

            val msg = context.getTranslation("$root.response1")
                .withVariable(PLACEHOLDER_PREFIX, prefix)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r", "delete", "d")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val prefix = context.rawArg
            context.daoManager.guildPrefixWrapper.removePrefix(context.guildId, prefix)

            val msg = context.getTranslation("$root.response1")
                .withVariable(PLACEHOLDER_PREFIX, prefix)
            sendRsp(context, msg)
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "deleteAt", "dat")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.guildPrefixWrapper
            val list = wrapper.getPrefixes(context.guildId)
            val index = getIntegerFromArgNMessage(context, 0, 0, list.size - 1) ?: return

            val toRemove = list[index]
            wrapper.removePrefix(context.guildId, toRemove)

            val msg = context.getTranslation("$root.removed")
                .withVariable(PLACEHOLDER_PREFIX, toRemove)
            sendRsp(context, msg)
        }
    }
}