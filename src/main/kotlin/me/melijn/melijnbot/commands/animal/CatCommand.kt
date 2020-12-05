package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class CatCommand : AbstractCommand("command.cat") {

    init {
        id = 46
        name = "cat"
        aliases = arrayOf("meow")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomCatUrl(web, context.container.settings.tokens.randomCatApi))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomCatUrl(webManager: WebManager, apiKey: String): String {
        val headers = mapOf(
            "x-api-key" to apiKey
        )

        val params = mapOf(
            "limit" to "1",
            "format" to "json",
            "order" to "RANDOM"
        )

        val reply = WebUtils.getJsonAFromUrl(
            webManager.httpClient,
            "https://api.thecatapi.com/v1/images/search",
            params,
            headers
        )
            ?: return MISSING_IMAGE_URL
        return reply.getObject(0).getString("url", MISSING_IMAGE_URL)
    }
}