package me.melijn.melijnbot.commandutil.anime

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.getRoleByArgsN
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsN
import me.melijn.melijnbot.internals.utils.withVariable
import me.melijn.melijnbot.internals.web.apis.WeebApi
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User

object AnimeCommandUtil {

    suspend fun execute(
        context: ICommandContext,
        type: String,
        apiOrder: Array<WeebApi.Type> = emptyArray()
    ) {
        val author: User?
        val authorRole: Role?
        val target: User?
        val targetRole: Role?
        when {
            context.args.isEmpty() -> {
                author = context.author
                executeAbs(context, type, apiOrder, author, null)
            }
            context.args.size == 1 -> {
                author = context.author
                target = retrieveUserByArgsN(context, 0)
                targetRole = getRoleByArgsN(context, 0)
                when {
                    targetRole != null -> executeAbs(context, type, apiOrder, author, targetRole)
                    target != null -> executeAbs(context, type, apiOrder, author, target)
                    else -> {
                        val msg = context.getTranslation("message.unknown.userorrole")
                            .withVariable(PLACEHOLDER_ARG, context.args[0])
                        sendRsp(context, msg)
                    }
                }
            }
            else -> {
                author = retrieveUserByArgsN(context, 0)
                authorRole = getRoleByArgsN(context, 0)
                target = retrieveUserByArgsN(context, 1)
                targetRole = getRoleByArgsN(context, 1)
                when {
                    author != null -> {
                        when {
                            target != null -> executeAbs(context, type, apiOrder, author, target)
                            targetRole != null -> executeAbs(context, type, apiOrder, author, targetRole)
                            else -> {
                                val msg = context.getTranslation("message.unknown.userorrole")
                                    .withVariable(PLACEHOLDER_ARG, context.args[1])
                                sendRsp(context, msg)
                            }
                        }
                    }
                    authorRole != null -> {
                        when {
                            target != null -> executeAbs(context, type, apiOrder, authorRole, target)
                            targetRole != null -> executeAbs(context, type, apiOrder, authorRole, targetRole)
                            else -> {
                                val msg = context.getTranslation("message.unknown.userorrole")
                                    .withVariable(PLACEHOLDER_ARG, context.args[1])
                                sendRsp(context, msg)
                            }
                        }
                    }
                    else -> {
                        val msg = context.getTranslation("message.unknown.userorrole")
                            .withVariable(PLACEHOLDER_ARG, context.args[0])
                        sendRsp(context, msg)
                    }
                }
            }
        }
    }

    private suspend fun executeAbs(
        context: ICommandContext,
        type: String,
        apiOrder: Array<WeebApi.Type>,
        author: User,
        target: User?
    ) {
        if (context.isFromGuild) {
            val authorMember = context.guild.retrieveMember(author).awaitOrNull() ?: return
            val targetMember = target?.let { context.guild.retrieveMember(it).awaitOrNull() }
            executeAbs(
                context,
                type,
                authorMember.effectiveName,
                targetMember?.effectiveName ?: target?.name ?: ""
            )
        } else {
            executeAbs(context, type, author.name, target?.name ?: "")
        }
    }

    private suspend fun executeAbs(
        context: ICommandContext,
        type: String,
        apiOrder: Array<WeebApi.Type>,
        author: Role,
        target: User?
    ) {
        val targetMember = target?.let { context.guild.retrieveMember(it).awaitOrNull() }
        executeAbs(context, type, author.asMention, targetMember?.effectiveName ?: target?.name ?: "")
    }

    private suspend fun executeAbs(
        context: ICommandContext,
        type: String,
        apiOrder: Array<WeebApi.Type>,
        author: User,
        target: Role
    ) {
        val authorMember = context.guild.retrieveMember(author).awaitOrNull() ?: return
        executeAbs(context, type, authorMember.effectiveName, target.asMention)
    }

    private suspend fun executeAbs(
        context: ICommandContext, type: String, apiOrder: Array<WeebApi.Type>, author: Role, target: Role
    ) {
        executeAbs(context, type, author.asMention, target.asMention)
    }

    private suspend fun executeAbs(
        context: ICommandContext, type: String, author: String,
        target: String
    ) {
        val path = context.commandOrder.last().root + if (target.isEmpty()) {
            ".eb.description.solo"
        } else {
            ".eb.description"
        }
        val title = context.getTranslation(path)
            .withVariable("author", author)
            .withVariable("target", target)

        val eb = Embedder(context)
            .setDescription(title)
            .setImage(context.webManager.weebApi.getUrlRandom(type, false))
        sendEmbedRsp(context, eb.build())
    }

    suspend fun executeShow(context: ICommandContext, type: String, nsfw: Boolean = false) {
        val eb = Embedder(context)
        if (nsfw && context.isFromGuild && context.textChannel.isNSFW) {
            eb.setImage(context.webManager.weebApi.getUrlRandom(type, nsfw))
        } else {
            eb.setImage(context.webManager.weebApi.getUrlRandom(type))
        }
        sendEmbedRsp(context, eb.build())
    }
}