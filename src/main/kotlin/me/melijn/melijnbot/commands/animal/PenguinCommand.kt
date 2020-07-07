package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.message.sendEmbedRsp
import me.melijn.melijnbot.objects.web.WebManager
import me.melijn.melijnbot.objects.web.WebUtils

class PenguinCommand : AbstractCommand("command.penguin") {

    init {
        id = 182
        name = "penguin"
        aliases = arrayOf("businessbird")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomPandaUrl(web, context.container.settings.melijnCDN.token))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomPandaUrl(webManager: WebManager, token: String): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://cdnapi.melijn.com/img/penguin", headers =
        mapOf(Pair("token", token))) ?: return MISSING_IMAGE_URL
        return reply.getString("url")
    }
}