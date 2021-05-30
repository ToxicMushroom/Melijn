package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class NyancatCommand : AbstractCommand("command.nyancat") {

    init {
        id = 49
        name = "nyancat"
        aliases = arrayOf("nyan", "nya")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: ICommandContext) {
        val title = context.getTranslation("$root.title")

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomNyancatUrl(context.webManager, context.container.settings.api.imgHoard.token))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomNyancatUrl(webManager: WebManager, token: String): String {
        val reply = WebUtils.getJsonFromUrl(
            webManager.httpClient, "https://api.miki.bot/images/random?tags=nyancat",
            headers = mapOf(Pair("Authorization", token))
        ) ?: return MISSING_IMAGE_URL
        return reply.getString("url")
    }
}