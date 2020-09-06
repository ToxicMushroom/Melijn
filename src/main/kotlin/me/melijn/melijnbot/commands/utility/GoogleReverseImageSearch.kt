package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getImageUrlFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp

class GoogleReverseImageSearch : AbstractCommand("command.googlereverseimagesearch") {

    init {
        id = 214
        name = "googleReverseImageSearch"
        aliases = arrayOf("gris")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val attachment = (getImageUrlFromArgsNMessage(context, 0) ?: return).second
        val eb = Embedder(context)
            .setDescription("[view result](https://www.google.com/searchbyimage?image_url=$attachment)")
        sendEmbedRsp(context, eb.build())
    }
}