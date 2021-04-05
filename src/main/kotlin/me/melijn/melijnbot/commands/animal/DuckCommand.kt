package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class DuckCommand : AbstractCommand("command.duck") {

    init {
        id = 144
        name = "duck"
        aliases = arrayOf("quack", "honk", "cannard")
        commandCategory = CommandCategory.ANIMAL
    }


    suspend fun execute(context: ICommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomDuckUrl(web))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomDuckUrl(webManager: WebManager): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://random-d.uk/api/v2/random")
            ?: return MISSING_IMAGE_URL
        return reply.getString("url", MISSING_IMAGE_URL)
    }
}