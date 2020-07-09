package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils

class DogCommand : AbstractCommand("command.dog") {

    init {
        id = 47
        name = "dog"
        aliases = arrayOf("woof")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomDogUrl(web))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomDogUrl(webManager: WebManager): String {
        val reply = WebUtils.getJsonFromUrl(webManager.httpClient, "https://random.dog/woof.json")
            ?: return MISSING_IMAGE_URL
        return reply.getString("url")
    }
}