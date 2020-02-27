package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.sendEmbed
import kotlin.random.Random

class AIWaifuCommand : AbstractCommand("command.aiwaifu") {

    init {
        id = 145
        name = "aiWaifu"
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
        val title = context.getTranslation("$root.title")

        eb.setTitle(title, "https://www.gwern.net/TWDNE")
        eb.setImage(getRandomAiWaifuUrl())
        sendEmbed(context, eb.build())
    }

    private fun getRandomAiWaifuUrl(): String {
        return "https://www.thiswaifudoesnotexist.net/v2/example-${Random.nextInt(0, 190_000)}.jpg"
    }
}