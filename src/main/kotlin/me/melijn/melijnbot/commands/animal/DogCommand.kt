package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.web.WebManager

class DogCommand : AbstractCommand("command.dog") {

    init {
        id = 47
        name = "dog"
        aliases = arrayOf("woof")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
        val title = context.getTranslation("$root.title")

        val web = context.webManager
        eb.setTitle(title)
        eb.setImage(getRandomDogUrl(web))
        sendEmbed(context, eb.build())
    }

    private suspend fun getRandomDogUrl(webManager: WebManager): String {
        val reply = webManager.getJsonFromUrl("https://random.dog/woof.json") ?: return MISSING_IMAGE_URL
        return reply.getString("url")
    }
}