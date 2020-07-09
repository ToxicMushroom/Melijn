package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.isPremiumUser
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

const val PRIVATE_PREFIXES_LIMIT = 1
const val PREMIUM_PRIVATE_PREFIXES_LIMIT = 10
const val PRIVATE_PREFIXES_LIMIT_PATH = "premium.feature.privateprefix.limit"

class PrivatePrefixesCommand : AbstractCommand("command.privateprefixes") {

    init {
        id = 19
        name = "privatePrefixes"
        aliases = arrayOf("pp", "privatePrefix")
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
            aliases = arrayOf("ls", "view")
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
            sendRsp(context, msg)
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

            val wrapper = context.daoManager.userPrefixWrapper
            val ppList = wrapper.prefixCache[context.guildId].await()
            if (ppList.size >= PRIVATE_PREFIXES_LIMIT && !isPremiumUser(context)) {
                val replaceMap = mapOf(
                    "limit" to "$PRIVATE_PREFIXES_LIMIT",
                    "premiumLimit" to "$PREMIUM_PRIVATE_PREFIXES_LIMIT"
                )

                sendFeatureRequiresPremiumMessage(context, PRIVATE_PREFIXES_LIMIT_PATH, replaceMap)
                return
            } else if (ppList.size >= PREMIUM_PRIVATE_PREFIXES_LIMIT) {
                val msg = context.getTranslation("$root.limit")
                    .withVariable("limit", "$PREMIUM_PRIVATE_PREFIXES_LIMIT")
                sendRsp(context, msg)
                return
            }

            val prefix = context.rawArg
            context.daoManager.userPrefixWrapper.addPrefix(context.authorId, prefix)

            val msg = context.getTranslation("$root.response1")
                .withVariable(PLACEHOLDER_PREFIX, prefix)
            sendRsp(context, msg)
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
                .withVariable(PLACEHOLDER_PREFIX, prefix)
            sendRsp(context, msg)
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
                .withVariable(PLACEHOLDER_PREFIX, toRemove)
            sendRsp(context, msg)
        }
    }
}