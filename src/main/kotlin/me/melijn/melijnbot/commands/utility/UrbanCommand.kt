package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.remove
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils
import java.lang.Integer.min

class UrbanCommand : AbstractCommand("command.urban") {

    init {
        id = 110
        name = "urban"
        aliases = arrayOf("urbanDictionary")
        runConditions = arrayOf(RunCondition.CHANNEL_NSFW)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val web = context.webManager
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val result = getUrbanResult(web, context.rawArg)
        val first = result?.first
        val second = result?.second

        if (result == null) {
            val offline = context.getTranslation("$root.urbanoffline")
            sendRsp(context, offline)

        } else if (result.first == null && result.second == null) {
            val notfound = context.getTranslation("$root.notfound")
                .withSafeVariable(PLACEHOLDER_ARG, context.rawArg)
            sendRsp(context, notfound)

        } else {
            val meaning = context.getTranslation("$root.meaning")
            val example = context.getTranslation("$root.example")

            val actualMeaning = if (first == null || first.isEmpty()) "/" else first.substring(0, min(1000, first.length))
            val actualExample = if (second == null || second.isEmpty()) "/" else second.substring(0, min(1000, second.length))


            val desc = "$meaning\n$actualMeaning\n\n$example\n$actualExample"
            val eb = Embedder(context)
                .setTitle(context.rawArg)
                .setDescription(desc)

            sendEmbedRsp(context, eb.build())
        }
    }

    private suspend fun getUrbanResult(webManager: WebManager, arg: String): Pair<String?, String?>? {
        val json = WebUtils.getJsonFromUrl(webManager.httpClient, "https://api.urbandictionary.com/v0/define?term=$arg")
            ?: return null
        val results = json.getArray("list")
        if (results.isEmpty) return Pair(null, null)
        val result = results.getObject(0)

        return Pair(
            result.getString("definition")
                .remove("[")
                .remove("]"),
            result.getString("example")
                .remove("]")
                .remove("[")
        )
    }
}