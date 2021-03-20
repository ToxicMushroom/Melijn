package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveMemberByArgsNMessage
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import java.awt.Color
import java.awt.Font
import java.awt.geom.Ellipse2D
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

class ShipCommand : AbstractCommand("command.ship") {

    init {
        id = 229
        name = "ship"
        aliases = arrayOf("loveCalc", "calcLove")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        } else {
            val user1: User
            val user2: User
            if (context.args.size > 1) {
                user1 = (retrieveMemberByArgsNMessage(context, 0) ?: return).user
                user2 = (retrieveMemberByArgsNMessage(context, 1) ?: return).user
            } else {
                user1 = context.author
                user2 = (retrieveMemberByArgsNMessage(context, 0) ?: return).user
            }

            val name1 = user1.idLong
            val name2 = user2.idLong

            val avatar1 = ImageIO.read(URL(user1.effectiveAvatarUrl.replace(".gif", ".png") + "?size=256"))
            val avatar2 = ImageIO.read(URL(user2.effectiveAvatarUrl.replace(".gif", ".png") + "?size=256"))
            val result = round((name1 + name2).toString().takeLast(3).toDouble() / 10.0)
            val bg = ShipCommand::class.java.getResourceAsStream("/love.png").use { ImageIO.read(it) }
            val graphics = bg.graphics
            graphics.clip = Ellipse2D.Float(124f, 175f, 251f, 251f)
            graphics.drawImage(avatar1, 124, 175, 251, 251, null)
            graphics.clip = Ellipse2D.Float(624f, 175f, 251f, 251f)
            graphics.drawImage(avatar2, 624, 175, 251, 251, null)
            graphics.clip = null

            val length = 95
            val angle = Math.PI - ((result / 100) * Math.PI)
            val ex = cos(angle) * length
            val ey = sin(angle) * length
            graphics.color = Color.RED
            graphics.drawLine(500, 528, (500 + ex).toInt(), (528 - ey).toInt())
            graphics.drawLine(499, 528, (499 + ex).toInt(), (528 - ey).toInt())
            graphics.drawLine(501, 528, (501 + ex).toInt(), (528 - ey).toInt())
            graphics.drawLine(502, 528, (502 + ex).toInt(), (528 - ey).toInt())
            graphics.drawLine(498, 528, (498 + ex).toInt(), (528 - ey).toInt())


            val font = graphics.font.deriveFont(30f)
            graphics.font = font
            graphics.color = Color(0x222222)
            val zeroTextWidth = graphics.fontMetrics.stringWidth("0") / 2
            graphics.drawString("0", 350 - zeroTextWidth, 534)
            graphics.drawString("100", 640, 534)

            // percentage text
            val font2 = graphics.font.deriveFont(Font.BOLD, 60f)
            graphics.font = font2
            val halfLetter2 = graphics.fontMetrics.height / 2
            val halfTextWidth = graphics.fontMetrics.stringWidth("${result.toInt()}%") / 2
            graphics.drawString("${result.toInt()}%", 500 - halfTextWidth, 330 + halfLetter2)

            val font3 = graphics.font.deriveFont(30f)
            graphics.font = font3
            val halfLetter3 = graphics.fontMetrics.height / 2
            val halfName1Width = graphics.fontMetrics.stringWidth(user1.name) / 2
            graphics.drawString(user1.name, 250 - halfName1Width, 150 + halfLetter3)
            val halfName2Width = graphics.fontMetrics.stringWidth(user2.name) / 2
            graphics.drawString(user2.name, 750 - halfName2Width, 150 + halfLetter3)

            graphics.dispose()

            sendRsp(context, bg, "png")
        }
    }
}