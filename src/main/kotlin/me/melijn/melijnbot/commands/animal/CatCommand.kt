package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.web.WebManager

class CatCommand : AbstractCommand("command.cat") {

    init {
        id = 46
        name = "cat"
        aliases = arrayOf("meow")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
        val language = context.getLanguage()
        val title = i18n.getTranslation(language, "$root.title")

        val web = context.webManager
        eb.setTitle(title)
        eb.setImage(getRandomCatUrl(web))
        sendEmbed(context, eb.build())
    }

    private suspend fun getRandomCatUrl(webManager: WebManager): String {
        val reply = webManager.getJsonFromUrl("http://aws.random.cat/meow")?: return MISSING_IMAGE_URL
        return reply.getString("link")
    }
}