package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class KoalaCommand : AbstractCommand("command.koala") {

    init {
        id = 51
        name = "koala"
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomKoalaUrl(web))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomKoalaUrl(webManager: WebManager): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://some-random-api.ml/img/koala")
            ?: return MISSING_IMAGE_URL
        return reply.getString("link")
    }
}