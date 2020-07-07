package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.asLongLongGMTString
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.message.sendSyntax
import me.melijn.melijnbot.objects.utils.withVariable
import net.dv8tion.jda.api.entities.Emote

class EmoteCommand : AbstractCommand("command.emote") {

    init {
        id = 13
        name = "emote"
        aliases = arrayOf("emoji")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val args = context.rawArg
        if (args.isBlank()) {
            sendSyntax(context)
            return
        }

        val language = context.getLanguage()

        val trans1 = i18n.getTranslation(language, "$root.response1.part1")
        val trans2 = i18n.getTranslation(language, "$root.response1.part2")
        val transExtra = i18n.getTranslation(language, "$root.response1.extra")
        val id: String
        var emote: Emote? = null
        if (args.matches("<.?:.*:\\d+>".toRegex())) {
            id = args.replace("<.?:.*:(\\d+)>".toRegex(), "$1")
            emote = context.shardManager.getEmoteById(id)

            if (emote == null) {
                val name = args.replace("<.?:(.*):\\d+>".toRegex(), "$1")
                val animated = args.replace("<(.?):.*:\\d+>".toRegex(), "$1").isNotEmpty()
                val msg = replaceMissingEmoteVars(
                    trans1 + trans2,
                    context,
                    id,
                    name,
                    animated
                )

                sendRsp(context, msg)
                return
            }
        } else if (args.matches("\\d+".toRegex())) {
            id = args
            emote = context.shardManager.getEmoteById(id)
        }

        if (emote == null) {
            val msg = i18n.getTranslation(language, "$root.notanemote")
                .withVariable(PLACEHOLDER_ARG, args)
            sendRsp(context, msg)
            return
        }

        val msg = replaceEmoteVars(
            trans1 + transExtra + trans2,
            context,
            emote
        )
        sendRsp(context, msg)
        return

    }


    private suspend fun replaceMissingEmoteVars(string: String, context: CommandContext, id: String, name: String, animated: Boolean): String = string
        .withVariable("id", id)
        .withVariable("name", name)
        .withVariable("isAnimated", i18n.getTranslation(context.getLanguage(), if (animated) "yes" else "no"))
        .withVariable("url", "https://cdn.discordapp.com/emojis/$id." + (if (animated) "gif" else "png") + "?size=2048")


    private suspend fun replaceEmoteVars(string: String, context: CommandContext, emote: Emote): String =
        replaceMissingEmoteVars(string, context, emote.id, emote.name, emote.isAnimated)
            .withVariable("creationTime", emote.timeCreated.asLongLongGMTString())

}