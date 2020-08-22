package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.withVariable

class InviteCommand : AbstractCommand("command.invite") {

    init {
        id = 98
        name = "invite"
        aliases = arrayOf("inviteLink", "inviteBot")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val botId = context.jda.selfUser.idLong
        val baseUrl = "https://discordapp.com/oauth2/authorize?client_id=$botId&scope=bot"
        val title = context.getTranslation("$root.title")
            .withVariable("botName", context.selfMember.user.name)
        val msg = context.getTranslation("$root.desc")
            .withVariable("urlWithPerm", "$baseUrl&permissions=322268358")
            .withVariable("urlWithoutPerm", baseUrl)

        val eb = Embedder(context)
            .setTitle(title)
            .setDescription(msg)

        sendEmbedRsp(context, eb.build())
    }
}