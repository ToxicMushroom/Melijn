package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER_ID
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg

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
                .replace("%url%", VOTE_URL)
            sendMsg(context, msg)
            return
        }


        val fieldTitle = context.getTranslation("$root.field.voteinfo")
        val value = context.getTranslation("$root.field.value")
            .replace(PLACEHOLDER_USER, target.asTag)
            .replace(PLACEHOLDER_USER_ID, target.id)
            .replace("%votes%", userVote.votes.toString())
            .replace("%streak%", userVote.streak.toString())
            .replace("%lastTime%", userVote.lastTime.asEpochMillisToDateTime(context.getTimeZoneId()))

        val eb = Embedder(context)
        eb.setThumbnail(target.effectiveAvatarUrl)
        eb.addField(fieldTitle, value, true)
        sendEmbed(context, eb.build())
    }
}