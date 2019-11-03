package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.web.WebManager
import java.lang.Integer.min

class UrbanCommand : AbstractCommand("command.urban") {

    init {
        id = 110
        name = "urban"
        aliases = arrayOf("urbanDictionary")
        runConditions = arrayOf(RunCondition.CHANNEL_NSFW)
    }

    override suspend fun execute(context: CommandContext) {
        val language = context.getLanguage()
        val web = context.webManager
        val result = getUrbanResult(web, context.rawArg)
        val first = result?.first
        val second = result?.second

        if (result == null) {
            val offline = i18n.getTranslation(language, "$root.urbanoffline")
            sendMsg(context, offline)
        } else if (result.first == null && result.second == null) {
            val notfound = i18n.getTranslation(language, "$root.notfound")
            sendMsg(context, notfound)
        } else {
            val meaning = i18n.getTranslation(language, "$root.meaning")
            val example = i18n.getTranslation(language, "$root.example")

            val actualMeaning = if (first == null || first.isEmpty()) "/" else first.substring(0, min(1000, first.length))
            val actualExample = if (second == null || second.isEmpty()) "/" else second.substring(0, min(1000, second.length))

            val eb = Embedder(context)
            val desc = "$meaning\n$actualMeaning\n\n$example\n$actualExample"
            eb.setTitle(context.rawArg)
            eb.setDescription(desc)
            sendEmbed(context, eb.build())
        }
    }

    private suspend fun getUrbanResult(webManager: WebManager, arg: String): Pair<String?, String?>? {
        val json = webManager.getJsonFromUrl("https://api.urbandictionary.com/v0/define?term=$arg") ?: return null
        val results = json.getArray("list")
        if (results.isEmpty) return Pair(null, null)
        val result = results.getObject(0)
        return Pair(result.getString("definition"), result.getString("example"))
    }
}