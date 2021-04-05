package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getImageUrlFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import net.dv8tion.jda.api.utils.MarkdownSanitizer

class GoogleReverseImageSearchCommand : AbstractCommand("command.googlereverseimagesearch") {

    init {
        id = 214
        name = "googleReverseImageSearch"
        aliases = arrayOf("gris")
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(context: ICommandContext) {
        val attachment = (getImageUrlFromArgsNMessage(context, 0) ?: return).second

        val eb = Embedder(context)
            .setDescription(
                "[view result](https://www.google.com/searchbyimage?image_url=${
                    MarkdownSanitizer.escape(attachment)
                        .replace(")", "%29")
                        .replace("(", "%28")
                })"
            )
        sendEmbedRsp(context, eb.build())
    }
}