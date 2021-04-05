package me.melijn.melijnbot.commands.nsfw

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.SPACE_REGEX
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.MessageEmbed

class TBibCommand : AbstractCommand("command.tbib") {

    init {
        id = 232
        name = "tbib"
        cooldown = 1000
        commandCategory = CommandCategory.NSFW
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        context.initCooldown()

        val search = context.fullArg.take(124)
        val tags = search.replace(SPACE_REGEX, "_").replace(",", "+")
        val result = context.webManager.booruApi.getRandomPost("tbib.org", tags)
        if (result == null) {
            sendRsp(context, "There were no results the tags you entered.")
            return
        }

        val eb = Embedder(context)
            .setTitle("TBib: $tags".take(MessageEmbed.TITLE_MAX_LENGTH))
            .setImage(result.imageUrl)
            .setFooter("Requested by: " + context.authorId)
            .build()

        sendEmbedRsp(context, eb)
    }
}