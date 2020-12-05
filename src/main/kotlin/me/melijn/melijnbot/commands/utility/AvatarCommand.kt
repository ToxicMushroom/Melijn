package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withSafeVariable

class AvatarCommand : AbstractCommand("command.avatar") {

    init {
        id = 122
        name = "avatar"
        aliases = arrayOf("ava", "pfp", "av")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val user = if (context.args.isEmpty()) {
            context.author
        } else {
            retrieveUserByArgsNMessage(context, 0) ?: return
        }

        val avatar = user.effectiveAvatarUrl + "?size=2048"

        val title = context.getTranslation("$root.title")
            .withSafeVariable(PLACEHOLDER_USER, user.asTag)

        val links = context.getTranslation("$root.links")

        val eb = Embedder(context)
            .setTitle(title)
            .setImage(avatar)
            .setDescription(
                links + " **" +
                    "[direct](${user.effectiveAvatarUrl}) • " +
                    "[x64](${user.effectiveAvatarUrl}?size=64) • " +
                    "[x128](${user.effectiveAvatarUrl}?size=128) • " +
                    "[x256](${user.effectiveAvatarUrl}?size=256) • " +
                    "[x512](${user.effectiveAvatarUrl}?size=512) • " +
                    "[x1024](${user.effectiveAvatarUrl}?size=1024) • " +
                    "[x2048](${user.effectiveAvatarUrl}?size=2048)**"
            )

        sendEmbedRsp(context, eb.build())
    }
}