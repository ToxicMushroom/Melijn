package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withVariable

class VoteInfoCommand : AbstractCommand("command.voteinfo") {

    init {
        id = 117
        name = "voteInfo"
        aliases = arrayOf("voteStats", "vi")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val voteWrapper = context.daoManager.voteWrapper
        val target = if (context.args.isEmpty()) {
            context.author
        } else {
            retrieveUserByArgsNMessage(context, 0) ?: return
        }

        val userVote = voteWrapper.getUserVote(target.idLong)
        val title = context.getTranslation("$root.embed.title")
            .withVariable(PLACEHOLDER_USER, target.asTag)
        val untilNext = (userVote?.lastTime ?: 0) - (System.currentTimeMillis() - (12 * 3600_000))
        val nextString = if (untilNext <= 0) {
            context.getTranslation("ready")
        } else {
            context.getTranslation("$root.readyin")
                .withVariable("duration", getDurationString(untilNext))
        }

        val description = context.getTranslation("$root.embed.description")
            .withVariable("votes", userVote?.votes ?: 0)
            .withVariable("streak", userVote?.streak ?: 0)
            .withVariable("next", nextString)
            .withVariable("lastTime", userVote?.lastTime?.asEpochMillisToDateTime(context.getTimeZoneId()) ?: "/")

        val eb = Embedder(context)
            .setThumbnail(target.effectiveAvatarUrl)
            .setTitle(title, VOTE_URL)
            .setDescription(description)
        sendEmbedRsp(context, eb.build())
    }
}