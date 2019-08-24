package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.asFullLongGMTString
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.entities.Emote

class EmoteCommand : AbstractCommand("command.emote") {

    init {
        id = 13
        name = "emote"
        aliases = arrayOf("emoji")
        commandCategory = CommandCategory.UTILITY
    }

    override fun execute(context: CommandContext) {
        val args = context.rawArg

        if (args.isBlank()) {
            sendSyntax(context, syntax)
            return
        }

        val part1 = Translateable("$root.response1.part1")
        val part2 = Translateable("$root.response1.part2")
        val extra = Translateable("$root.response1.extra")
        val id: String
        var emote: Emote? = null
        if (args.matches("<.?:.*:\\d+>".toRegex())) {
            id = args.replace("<.?:.*:(\\d+)>".toRegex(), "$1")
            emote = context.getShardManager()?.getEmoteById(id)

            if (emote == null) {
                val name = args.replace("<.?:(.*):\\d+>".toRegex(), "$1")
                val animated = args.replace("<(.?):.*:\\d+>".toRegex(), "$1").isNotEmpty()
                sendMsg(context,
                        replaceMissingEmoteVars(
                                part1.string(context) + part2.string(context),
                                context,
                                id,
                                name,
                                animated
                        )
                )
                return
            }
        } else if (args.matches("\\d+".toRegex())) {
            id = args
            emote = context.getShardManager()?.getEmoteById(id)
        }

        if (emote == null) {
            sendMsg(context, Translateable("$root.notanemote").string(context)
                    .replace(PLACEHOLDER_ARG, args)
            )
            return
        }

        sendMsg(context, replaceEmoteVars(
                part1.string(context) + extra.string(context) + part2.string(context),
                context,
                emote
        ))
        return

    }

    private fun replaceMissingEmoteVars(string: String, context: CommandContext, id: String, name: String, animated: Boolean): String {
        return string
                .replace("%id%", id)
                .replace("%name%", name)
                .replace("%isAnimated%", Translateable(if (animated) "yes" else "no").string(context))
                .replace("%url%", "https://cdn.discordapp.com/emojis/$id." + (if (animated) "gif" else "png") + "?size=2048")
    }

    fun replaceEmoteVars(string: String, context: CommandContext, emote: Emote): String {
        return replaceMissingEmoteVars(string, context, emote.id, emote.name, emote.isAnimated)
                .replace("%creationTime%", emote.timeCreated.asFullLongGMTString())
    }
}