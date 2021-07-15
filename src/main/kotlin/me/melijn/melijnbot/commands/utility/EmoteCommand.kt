package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.database.statesync.LiteEmote
import me.melijn.melijnbot.database.statesync.getEmote
import me.melijn.melijnbot.database.statesync.toLite
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.utils.TimeFormat

class EmoteCommand : AbstractCommand("command.emote") {

    init {
        id = 13
        name = "emote"
        aliases = arrayOf("emoji")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val trans1 = context.getTranslation("$root.response1.part1")
        val trans2 = context.getTranslation("$root.response1.part2")
        val transExtra = context.getTranslation("$root.response1.extra")

        var emote: LiteEmote? = getEmoteByArgsN(context, 0, false)?.toLite()
        val arg = context.args[0]

        if (emote == null && EMOTE_MENTION.matches(arg)) {
            val result = (EMOTE_MENTION.find(arg) ?: return).groupValues
            val emoteId = result[2].toLongOrNull() ?: return
            emote = getEmote(context, emoteId)

            if (emote == null) {
                val animated = arg.startsWith("<a")
                val msg = replaceMissingEmoteVars(
                    trans1 + trans2,
                    context,
                    emoteId,
                    result[1],
                    animated
                )

                sendRsp(context, msg)
                return
            }
        } else if (arg.isPositiveNumber()) {
            emote = context.shardManager.getEmoteById(arg)?.toLite()
        }

        if (emote != null) {
            val msg = replaceEmoteVars(
                trans1 + transExtra + trans2,
                context,
                emote
            )

            sendRsp(context, msg)

        } else {
            val msg = context.getTranslation("$root.notanemote")
                .withSafeVariable(PLACEHOLDER_ARG, arg)
            sendRsp(context, msg)
        }
    }

    private suspend fun replaceMissingEmoteVars(
        string: String,
        context: ICommandContext,
        id: Long,
        name: String,
        animated: Boolean
    ): String = string
        .withVariable("id", id)
        .withSafeVariable("name", name)
        .withVariable("isAnimated", context.getTranslation(if (animated) "yes" else "no"))
        .withVariable("url", "https://cdn.discordapp.com/emojis/$id." + (if (animated) "gif" else "png") + "?size=2048")

    private suspend fun replaceEmoteVars(string: String, context: ICommandContext, emote: LiteEmote): String =
        replaceMissingEmoteVars(string, context, emote.id, emote.name, emote.isAnimated)
            .withVariable("creationTime", TimeFormat.DATE_TIME_SHORT.atTimestamp(snowflakeToEpochMillis(emote.id)))

}

