package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.web.WebManager

class BirdCommand : AbstractCommand("command.bird") {

    init {
        id = 53
        name = "bird"
        aliases = arrayOf("birb")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
        val title = i18n.getTranslation(context, "$root.title")

        val web = context.webManager
        eb.setTitle(title)
        eb.setImage(getRandomBirdUrl(web))
        sendEmbed(context, eb.build())
    }

    private suspend fun getRandomBirdUrl(webManager: WebManager): String {
        val reply = webManager.getJsonFromUrl("https://some-random-api.ml/img/birb")?: return MISSING_IMAGE_URL
        return reply.getString("link")
    }
}