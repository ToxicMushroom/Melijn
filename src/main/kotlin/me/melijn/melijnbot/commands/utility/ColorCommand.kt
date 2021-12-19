package me.melijn.melijnbot.commands.utility

import com.sksamuel.scrimage.color.RGBColor
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getColorFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.toHex
import net.dv8tion.jda.api.Permission
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class ColorCommand : AbstractCommand("command.color") {

    init {
        id = 132
        name = "color"
        discordChannelPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
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

        val rgb = RGBColor(color.red, color.green, color.blue, color.transparency)
        val grayscale = rgb.toGrayscale()
        val hsl = rgb.toHSL()
        val cmyk = rgb.toCMYK()
        val hsbArr = Color.RGBtoHSB(color.red, color.green, color.blue, FloatArray(3))
        val hsbFormat = "(${(hsbArr[0] * 360).toInt()}, ${(hsbArr[1] * 100).toInt()}%, ${(hsbArr[2] * 100).toInt()}%)"

        val eb = Embedder(context)
            .setColor(color)
            .addField(hexTitle, color.toHex(), true)
            .addField(rgbTitle, "rgb(${color.red}, ${color.green}, ${color.blue})", true)
            .addField(decTitle, color.rgb.toString(), true)
            .addField(hsbTitle, hsbFormat, true)
            .addField("HSL info", "${hsl.hue.toInt()}, ${(hsl.saturation * 100).toInt()}%, ${(hsl.lightness * 100).toInt()}%", true)
            .addField(
                "CMYK info",
                "${(cmyk.c * 100).toInt()}, ${(cmyk.m * 100).toInt()}, ${(cmyk.y * 100).toInt()}, ${(cmyk.k * 100).toInt()}",
                true
            )
            .addField("Grayscale info", "${grayscale.gray}%", true)
            .setThumbnail("attachment://file.png")

        val bais = ByteArrayOutputStream()
        bais.use {
            ImageIO.write(ImageUtils.createPlane(64, color.rgb), "png", it)
        }

        context.channel.sendMessageEmbeds(eb.build())
            .addFile(bais.toByteArray(), "file.png")
            .queue()
    }
}