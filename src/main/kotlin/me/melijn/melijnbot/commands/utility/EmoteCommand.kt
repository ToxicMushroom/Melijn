package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.arguments.annotations.CommandArg
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDate
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.Emote

class EmoteCommand : AbstractCommand("command.emote") {

    init {
        id = 13
        name = "emote"
        aliases = arrayOf("emoji")
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(
        context: ICommandContext,
        @CommandArg(index = 0) emote: Emote
    ) {
        val part1 = context.getTranslation("$root.response1.part1")
        val part2 = context.getTranslation("$root.response1.part2")
        val transExtra = context.getTranslation("$root.response1.extra")

        val msg = replaceEmoteVars(
            part1 + transExtra + part2,
            context,
            emote
        )

        sendRsp(context, msg)
    }


    private suspend fun replaceMissingEmoteVars(
        string: String,
        context: ICommandContext,
        id: String,
        name: String,
        animated: Boolean
    ): String = string
        .withVariable("id", id)
        .withSafeVariable("name", name)
        .withVariable("isAnimated", context.getTranslation(if (animated) "yes" else "no"))
        .withVariable("url", "https://cdn.discordapp.com/emojis/$id." + (if (animated) "gif" else "png") + "?size=2048")


    private suspend fun replaceEmoteVars(string: String, context: ICommandContext, emote: Emote): String =
        replaceMissingEmoteVars(string, context, emote.id, emote.name, emote.isAnimated)
            .withVariable("creationTime", emote.timeCreated.asEpochMillisToDate(context.getTimeZoneId()))

}