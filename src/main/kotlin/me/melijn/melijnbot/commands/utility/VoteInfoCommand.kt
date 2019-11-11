package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.getUserByArgsNMessage
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
            getUserByArgsNMessage(context, 0) ?: return
        }

        val userVote = voteWrapper.getUserVote(target.idLong)
        if (userVote == null) {
            val extra = if (context.author == target) ".self" else ""
            val msg = context.getTranslation("$root$extra.novote")
                .replace("%url%", VOTE_URL)
            sendMsg(context, msg)
            return
        }


        val title = context.getTranslation("$root.title")
            .replace(PLACEHOLDER_USER, target.asTag)
        val userId = context.getTranslation("$root.field.userId")
        val votes = context.getTranslation("$root.field.votes")
        val streak = context.getTranslation("$root.field.streak")
        val lastTime = context.getTranslation("$root.field.lasttime")

        val eb = Embedder(context)
        eb.setTitle(title)
        eb.setThumbnail(target.effectiveAvatarUrl)
        eb.addField(votes, userVote.votes.toString(), true)
        eb.addField(streak, userVote.streak.toString(), true)
        eb.addField(lastTime, userVote.lastTime.asEpochMillisToDateTime(), true)
        eb.addField(userId, target.id, true)
        sendEmbed(context, eb.build())
    }
}