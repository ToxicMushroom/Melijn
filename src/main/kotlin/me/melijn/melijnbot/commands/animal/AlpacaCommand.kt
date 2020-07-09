package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class AlpacaCommand : AbstractCommand("command.alpaca") {

    init {
        id = 48
        name = "alpaca"
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomAlpacaUrl(web))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomAlpacaUrl(webManager: WebManager): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://apis.duncte123.me/alpaca")
            ?: return MISSING_IMAGE_URL
        return reply.getObject("data").getString("file")
    }
}