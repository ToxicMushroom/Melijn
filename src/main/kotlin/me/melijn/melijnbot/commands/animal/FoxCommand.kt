package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.message.sendEmbedRsp
import me.melijn.melijnbot.objects.web.WebManager
import me.melijn.melijnbot.objects.web.WebUtils

class FoxCommand : AbstractCommand("command.fox") {

    init {
        id = 52
        name = "fox"
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomFoxUrl(web))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomFoxUrl(webManager: WebManager): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://some-random-api.ml/img/fox")
            ?: return MISSING_IMAGE_URL
        return reply.getString("link")
    }
}