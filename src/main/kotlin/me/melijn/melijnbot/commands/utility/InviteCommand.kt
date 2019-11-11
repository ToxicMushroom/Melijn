package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.sendEmbed

class InviteCommand : AbstractCommand("command.invite") {

    init {
        id = 98
        name = "invite"
        aliases = arrayOf("inviteLink", "inviteBot")
    }

    override suspend fun execute(context: CommandContext) {
        val botId = context.jda.selfUser.idLong
        val baseUrl = "https://discordapp.com/oauth2/authorize?client_id=$botId&scope=bot"
        val title = context.getTranslation("$root.title")
            .replace("%botName%", context.container.settings.name)
        val msg = context.getTranslation("$root.desc")
            .replace("%urlWithPerm%", "$baseUrl&permissions=322268358")
            .replace("%urlWithoutPerm%", baseUrl)

        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setDescription(msg)

        sendEmbed(context, eb.build())
    }
}