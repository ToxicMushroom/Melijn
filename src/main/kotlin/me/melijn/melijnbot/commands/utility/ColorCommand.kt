package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.*

class ColorCommand : AbstractCommand("command.color") {
    init {
        id = 132
        name = "color"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val color = getColorFromArgNMessage(context, 0) ?: return

        val fieldTitle = context.getTranslation("$root.eb.hex.title")

        val eb = Embedder(context)
        eb.setColor(color)
        eb.addField(fieldTitle, color.toHex() , true)
        sendEmbed(context, eb.build())
    }
}