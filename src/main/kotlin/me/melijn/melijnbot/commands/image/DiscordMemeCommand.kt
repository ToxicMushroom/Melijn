package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendEmbed
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
        val eb = Embedder(context)
        val language = context.getLanguage()
        val title = i18n.getTranslation(language, "$root.title")

        val web = context.webManager
        eb.setTitle(title)
        eb.setImage(getRandomDiscordMemeUrl(web))
        sendEmbed(context, eb.build())
    }

    private suspend fun getRandomDiscordMemeUrl(webManager: WebManager): String {
        return webManager.weebshApi.getUrl("discord_memes")
    }
}