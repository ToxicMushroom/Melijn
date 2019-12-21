package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.getColorFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendSyntax
import me.melijn.melijnbot.objects.utils.toHex

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

        val hexTitle = context.getTranslation("$root.eb.hex.title")
        val rgbTitle = context.getTranslation("$root.eb.rgb.title")
        val decTitle = context.getTranslation("$root.eb.dec.title")

        val eb = Embedder(context)
        eb.setColor(color)
        eb.addField(hexTitle, color.toHex(), true)
        eb.addField(rgbTitle, "(${color.red}, ${color.green}, ${color.blue})", true)
        eb.addField(decTitle, color.rgb.toString(), true)
        sendEmbed(context, eb.build())
    }
}