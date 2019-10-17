package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendEmbed
import kotlin.random.Random


class NyancatCommand : AbstractCommand("command.nyancat") {

    init {
        id = 49
        name = "nyancat"
        aliases = arrayOf("nyan", "nya")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
        val language = context.getLanguage()
        val title = i18n.getTranslation(language, "$root.title")

        eb.setTitle(title)
        eb.setImage(getRandomNyancatUrl())
        sendEmbed(context, eb.build())
    }

    private fun getRandomNyancatUrl(): String {
        val randomInt = Random.nextInt(2, 33)
        return "https://github.com/ToxicMushroom/nyan-cats/raw/master/cat%20($randomInt).gif"
    }
}