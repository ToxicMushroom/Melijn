package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.web.WebManager
import me.melijn.melijnbot.objects.web.WebUtils

class DuckCommand : AbstractCommand("command.duck") {

    init {
        id = 144
        name = "duck"
        aliases = arrayOf("quack", "honk", "cannard")
        commandCategory = CommandCategory.ANIMAL
    }


    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
        val title = context.getTranslation("$root.title")

        val web = context.webManager
        eb.setTitle(title)
        eb.setImage(getRandomDuckUrl(web))
        sendEmbed(context, eb.build())
    }

    private suspend fun getRandomDuckUrl(webManager: WebManager): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://random-d.uk/api/v2/random")
            ?: return MISSING_IMAGE_URL
        return reply.getString("url", MISSING_IMAGE_URL)
    }
}