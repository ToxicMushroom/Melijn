package me.melijn.melijnbot.commands.animal

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.web.WebManager
import me.melijn.melijnbot.internals.web.WebUtils
import net.dv8tion.jda.api.exceptions.ParsingException

class FishCommand : AbstractCommand("command.fish") {

    init {
        id = 240
        name = "fish"
        aliases = arrayOf("vis", "blub", "🐟")
        commandCategory = CommandCategory.ANIMAL
    }

    override suspend fun execute(context: ICommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomFishUrl(web, context.container.settings.api.imgHoard.token))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomFishUrl(webManager: WebManager, token: String): String {
        val reply = WebUtils.getJsonFromUrl(
            webManager.httpClient, "https://api.miki.bot/images/random?tags=fish",
            headers = mapOf(Pair("Authorization", token))
        ) ?: return MISSING_IMAGE_URL
        return try {
            reply.getString("url")
        }  catch (t: ParsingException) {
            throw IllegalArgumentException("Couldn't parse: $reply")
        }
    }

}