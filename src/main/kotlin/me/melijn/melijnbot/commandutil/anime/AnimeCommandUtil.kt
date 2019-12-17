package me.melijn.melijnbot.commandutil.anime

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.getRoleByArgsN
import me.melijn.melijnbot.objects.utils.getUserByArgsN
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User

object AnimeCommandUtil {

    suspend fun execute(context: CommandContext, type: String) {
        val author: User?
        val authorRole: Role?
        val target: User?
        val targetRole: Role?
        when {
            context.args.isEmpty() -> {
                author = context.author
                executeAbs(context, type, author, null)
            }
            context.args.size == 1 -> {
                author = context.author
                target = getUserByArgsN(context, 0)
                targetRole = getRoleByArgsN(context, 0)
                when {
                    targetRole != null -> executeAbs(context, type, author, targetRole)
                    target != null -> executeAbs(context, type, author, target)
                    else -> {
                        val msg = context.getTranslation("message.unknown.userorrole")
                            .replace("%arg%", context.args[0])
                        sendMsg(context, msg)
                    }
                }
            }
            else -> {
                author = getUserByArgsN(context, 0)
                authorRole = getRoleByArgsN(context, 0)
                target = getUserByArgsN(context, 1)
                targetRole = getRoleByArgsN(context, 1)
                when {
                    author != null -> {
                        when {
                            target != null -> {
                                executeAbs(context, type, author, target)
                            }
                            targetRole != null -> {
                                executeAbs(context, type, author, targetRole)
                            }
                            else -> {
                                val msg = context.getTranslation("message.unknown.userorrole")
                                    .replace("%arg%", context.args[1])
                                sendMsg(context, msg)
                            }
                        }
                    }
                    authorRole != null -> {
                        when {
                            target != null -> {
                                executeAbs(context, type, authorRole, target)
                            }
                            targetRole != null -> {
                                executeAbs(context, type, authorRole, targetRole)
                            }
                            else -> {
                                val msg = context.getTranslation("message.unknown.userorrole")
                                    .replace("%arg%", context.args[1])
                                sendMsg(context, msg)
                            }
                        }
                    }
                    else -> {
                        val msg = context.getTranslation("message.unknown.userorrole")
                            .replace("%arg%", context.args[0])
                        sendMsg(context, msg)
                    }
                }
            }
        }
    }

    private suspend fun executeAbs(context: CommandContext, type: String, author: User, target: User?) {
        if (context.isFromGuild) {
            val authorMember = context.guild.getMember(author) ?: return
            val targetMember = target?.let { context.guild.getMember(it) }
            executeAbs(context, type, authorMember.effectiveName, targetMember?.effectiveName ?: target?.name ?: "")
        } else {
            executeAbs(context, type, author.name, target?.name ?: "")
        }
    }

    private suspend fun executeAbs(context: CommandContext, type: String, author: Role, target: User?) {
        val targetMember = target?.let { context.guild.getMember(it) }
        executeAbs(context, type, author.asMention, targetMember?.effectiveName ?: target?.name ?: "")
    }

    private suspend fun executeAbs(context: CommandContext, type: String, author: User, target: Role) {
        val authorMember = context.guild.getMember(author) ?: return
        executeAbs(context, type, authorMember.effectiveName, target.asMention)
    }


    private suspend fun executeAbs(context: CommandContext, type: String, author: Role, target: Role) {
        executeAbs(context, type, author.asMention, target.asMention)
    }

    private suspend fun executeAbs(context: CommandContext, type: String, author: String, target: String) {
        val path = context.commandOrder.last().root + if (target.isEmpty()) {
            ".eb.description.solo"
        } else {
            ".eb.description"
        }
        val title = context.getTranslation(path)
            .replace("%author%", author)
            .replace("%target%", target)


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