package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

const val PRIVATE_PREFIXES_LIMIT = 3
const val PREMIUM_PRIVATE_PREFIXES_LIMIT = 32
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

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "view")
        }

        override suspend fun execute(context: ICommandContext) {
            val title = context.getTranslation("$root.response1.title")

            var content = "```INI"
            val prefixes = context.daoManager.userPrefixWrapper.getPrefixes(context.authorId)
            for ((index, prefix) in prefixes.withIndex()) {
                content += "\n${index + 1} - [${prefix.escapeCodeblockMarkdown().escapeDiscordInvites()}]"
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

        override suspend fun execute(context: ICommandContext) {
            if (context.rawArg.isBlank()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.userPrefixWrapper
            val ppList = wrapper.getPrefixes(context.authorId)
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

            val prefix = context.rawArg.take((1024 - (PRIVATE_PREFIXES_LIMIT * "%SPLIT%".length)) / PREMIUM_PRIVATE_PREFIXES_LIMIT)
            context.daoManager.userPrefixWrapper.addPrefix(context.authorId, prefix)

            val msg = context.getTranslation("$root.response1")
                .withSafeVariable(PLACEHOLDER_PREFIX, prefix)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(root: String) : AbstractCommand("$root.remove") {

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
            context.daoManager.userPrefixWrapper.removePrefix(context.authorId, prefix)

            val msg = context.getTranslation("$root.response1")
                .withSafeVariable(PLACEHOLDER_PREFIX, prefix)
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

            val wrapper = context.daoManager.userPrefixWrapper
            val list = wrapper.getPrefixes(context.authorId)
            if (list.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendRsp(context, msg)
                return
            }

            val index = getIntegerFromArgNMessage(context, 0, 1, list.size) ?: return

            val toRemove = list[index - 1]
            wrapper.removePrefix(context.authorId, toRemove)


            val msg = context.getTranslation("$root.removed")
                .withSafeVariable(PLACEHOLDER_PREFIX, toRemove)
            sendRsp(context, msg)
        }
    }
}