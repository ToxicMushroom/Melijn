package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class PandaCommand : AbstractCommand("command.panda") {

    init {
        id = 50
        name = "panda"
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomPandaUrl(web))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomPandaUrl(webManager: WebManager): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://some-random-api.ml/img/panda")
            ?: return MISSING_IMAGE_URL
        return reply.getString("link")
    }
}