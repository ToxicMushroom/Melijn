package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed

class AvatarCommand : AbstractCommand("command.avatar") {

    init {
        id = 122
        name = "avatar"
        aliases = arrayOf("profilePicture", "pf")
    }

    override suspend fun execute(context: CommandContext) {
        val user = if (context.args.isEmpty()) {
            context.author
        } else {
            retrieveUserByArgsNMessage(context, 0) ?: return
        }
        val avatar = user.effectiveAvatarUrl + "?size=2048"

        val title = context.getTranslation("$root.title")
            .replace(PLACEHOLDER_USER, user.asTag)
        val links = context.getTranslation("$root.links")
        val embedder = Embedder(context)
        embedder.setTitle(title)
        embedder.setImage(avatar)
        embedder.setDescription(links + " **" +
            "[direct](${user.effectiveAvatarUrl}) • " +
            "[x64](${user.effectiveAvatarUrl}?size=64) • " +
            "[x128](${user.effectiveAvatarUrl}?size=128) • " +
            "[x256](${user.effectiveAvatarUrl}?size=256) • " +
            "[x512](${user.effectiveAvatarUrl}?size=512) • " +
            "[x1024](${user.effectiveAvatarUrl}?size=1024) • " +
            "[x2048](${user.effectiveAvatarUrl}?size=2048)**"
        )
        sendEmbed(context, embedder.build())
    }
}