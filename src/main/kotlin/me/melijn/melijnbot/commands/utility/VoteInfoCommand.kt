package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER_ID
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.message.sendEmbedRsp
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.withVariable

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
        if (userVote == null) {
            val extra = if (context.author == target) ".self" else ""
            val msg = context.getTranslation("$root$extra.novote")
                .withVariable("url", VOTE_URL)
            sendRsp(context, msg)
            return
        }


        val fieldTitle = context.getTranslation("$root.field.voteinfo")
        val value = context.getTranslation("$root.field.value")
            .withVariable(PLACEHOLDER_USER, target.asTag)
            .withVariable(PLACEHOLDER_USER_ID, target.id)
            .withVariable("votes", userVote.votes.toString())
            .withVariable("streak", userVote.streak.toString())
            .withVariable("lastTime", userVote.lastTime.asEpochMillisToDateTime(context.getTimeZoneId()))

        val eb = Embedder(context)
            .setThumbnail(target.effectiveAvatarUrl)
            .addField(fieldTitle, value, true)
        sendEmbedRsp(context, eb.build())
    }
}