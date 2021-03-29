package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getColorFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.toHex
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ColorCommand : AbstractCommand("command.color") {

    init {
        id = 132
        name = "color"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val color = getColorFromArgNMessage(context, 0) ?: return

        val hexTitle = context.getTranslation("$root.eb.hex.title")
        val rgbTitle = context.getTranslation("$root.eb.rgb.title")
        val decTitle = context.getTranslation("$root.eb.dec.title")
        val hsbTitle = context.getTranslation("$root.eb.hsb.title")

        val hsbArr = Color.RGBtoHSB(color.red, color.green, color.blue, FloatArray(3))
        val hsbFormat = "(${(hsbArr[0] * 360).toInt()}, ${(hsbArr[1] * 100).toInt()}%, ${(hsbArr[2] * 100).toInt()}%)"

        val eb = Embedder(context)
            .setColor(color)
            .addField(hexTitle, color.toHex(), true)
            .addField(rgbTitle, "(${color.red}, ${color.green}, ${color.blue})", true)
            .addField(decTitle, color.rgb.toString(), true)
            .addField(hsbTitle, hsbFormat, true)
            .setThumbnail("attachment://file.png")


        val bais = ByteArrayOutputStream()
        bais.use {
            ImageIO.write(ImageUtils.createPlane(64, color.rgb), "png", it)
        }

        context.channel.sendMessage(eb.build()).addFile(bais.toByteArray(), "file.png").queue()

    }
}