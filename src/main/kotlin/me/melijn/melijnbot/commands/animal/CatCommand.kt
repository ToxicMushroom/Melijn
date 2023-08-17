package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class CatCommand : AbstractCommand("command.cat") {

    init {
        id = 46
        name = "cat"
        aliases = arrayOf("meow", "🐈")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: ICommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomCatUrl(web, context.container.settings.api.imgHoard.token))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomCatUrl(webManager: WebManager, token: String): String {
        val reply = WebUtils.getJsonFromUrl(
            webManager.httpClient, "https://api.miki.bot/images/random?tags=cat",
            headers = mapOf(Pair("Authorization", token))
        ) ?: return MISSING_IMAGE_URL
        return reply.getString("url")
    }
}