package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.message.sendEmbedRsp
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.Permission

class DiscordMemeCommand : AbstractCommand("command.discordmeme") {

    init {
        id = 102
        name = "discordMeme"
        aliases = arrayOf("dmeme")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        val title = context.getTranslation("$root.title")
        val web = context.webManager

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(getRandomDiscordMemeUrl(web))
        sendEmbedRsp(context, eb.build())
    }

    private suspend fun getRandomDiscordMemeUrl(webManager: WebManager): String {
        return webManager.weebshApi.getUrl("discord_memes")
    }
}