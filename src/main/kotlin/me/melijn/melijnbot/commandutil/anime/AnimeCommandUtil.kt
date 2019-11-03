package me.melijn.melijnbot.commandutil.anime

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.User

object AnimeCommandUtil {

    suspend fun execute(context: CommandContext, type: String) {
        val author: User
        val target: User?
        when {
            context.args.isEmpty() -> {
                author = context.getAuthor()
                target = null
            }
            context.args.size == 1 -> {
                author = context.getAuthor()
                target = getUserByArgsNMessage(context, 0) ?: return
            }
            else -> {
                author = getUserByArgsNMessage(context, 0) ?: return
                target = getUserByArgsNMessage(context, 1) ?: return
            }
        }

        executeAbs(context, type, author, target)
    }

    private suspend fun executeAbs(context: CommandContext, type: String, author: User, target: User?) {
        val path = context.commandOrder.last().root + if (target == null){
            ".eb.description.solo"
        } else{
            ".eb.description"
        }
        var title = i18n.getTranslation(context, path)
        title = if (context.isFromGuild) {
            val authorMember = context.getGuild().getMember(author) ?: return
            val targetMember = target?.let { context.getGuild().getMember(it) }
            title
                .replace("%author%", authorMember.effectiveName)
                .replace("%target%", targetMember?.effectiveName ?: target?.name ?: "%target%")
        } else {
            title
                .replace("%author%", author.name)
                .replace("%target%", target?.name ?: "%target%")
        }


        val eb = Embedder(context)
        eb.setDescription(title)
        eb.setImage(context.webManager.getWeebJavaUrl(type))
        sendEmbed(context, eb.build())
    }

    suspend fun executeShow(context: CommandContext, type: String) {
        val eb = Embedder(context)
        eb.setImage(context.webManager.getWeebJavaUrl(type))
        sendEmbed(context, eb.build())
    }
}