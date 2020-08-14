package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import net.dv8tion.jda.api.entities.MessageEmbed
import kotlin.random.Random

class MemeCommand : AbstractCommand("command.meme") {

    init {
        id = 204
        name = "meme"
        aliases = arrayOf("dankmeme")
        commandCategory = CommandCategory.UTILITY
    }

    val reddits = arrayOf("dankmemes", "memes", "funny")

    override suspend fun execute(context: CommandContext) {
        val subreddit = if (context.commandParts[0] == "dankmeme") "dankmemes" else reddits[Random.nextInt(reddits.size)]

        val randomResult = RedditCommand.getRandomRedditResultNMessage(context, subreddit, "hot", "day") ?: return


        val embedder = Embedder(context)
            .setTitle(randomResult.title.take(256), "https://reddit.com" + randomResult.url)
            .setImage(if (randomResult.justText) null else randomResult.img)
            .setThumbnail(if (randomResult.justText) "https://cdn.melijn.com/img/11ixgBjie.png" else null)
            .setFooter("\uD83D\uDD3C ${randomResult.ups} | " + (randomResult.created * 1000).asEpochMillisToDateTime(context.getTimeZoneId()))
        if (randomResult.thumb.isNotBlank() && randomResult.justText) {
            embedder.setThumbnail(randomResult.thumb)
        }

        if (randomResult.justText) {
            embedder.setTitle(null, null)
                .setDescription(randomResult.title.take(MessageEmbed.TEXT_MAX_LENGTH - 256) + "\n[link](https://reddit.com${randomResult.url})")
        }

        sendEmbedRsp(context, embedder.build())
    }
}