package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.entities.Emote
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
                                Translateable("$root.response2").string(context),
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
                    .replace("%arg%", args)
            )
            return
        }

        sendMsg(context, replaceEmoteVars(
                Translateable("$root.response1").string(context),
                context,
                emote
        ))
        return

    }

    private fun replaceMissingEmoteVars(string: String, context: CommandContext, id: String, name: String, animated: Boolean): String {
        return string
                .replace("%emoteId%", id)
                .replace("%emoteName%", name)
                .replace("%isAnimated%", Translateable(if (animated) "yes" else "no").string(context))
                .replace("%url%", "https://discordapp.com/emoji/$id." + if (animated) "gif" else "png" + "?size=2048")
    }

    fun replaceEmoteVars(string: String, context: CommandContext, emote: Emote): String {
        return string
                .replace("%emoteId%", emote.id)
                .replace("%emoteName%", emote.name)
                .replace("%isAnimated%", Translateable(if (emote.isAnimated) "yes" else "no").string(context))
                .replace("%url%", "https://discordapp.com/emoji/${emote.id}." + if (emote.isAnimated) "gif" else "png" + "?size=2048")
                .replace("%creationTime%", emote.timeCreated.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG).withZone(ZoneId.of("GMT"))))
    }
}