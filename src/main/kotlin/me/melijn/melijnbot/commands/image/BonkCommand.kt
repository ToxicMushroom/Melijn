package me.melijn.melijnbot.commands.image

import com.squareup.gifencoder.*
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getBooleanFromArgN
import me.melijn.melijnbot.internals.utils.getLongFromArgN
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO

class BonkCommand : AbstractCommand("command.bonk") {

    init {
        id = 205
        name = "bonk"
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
        }

        val user = retrieveUserByArgsNMessage(context, 0) ?: return
        val inputImg = ImageIO.read(URL(user.effectiveAvatarUrl + "?size=512"))
        val delay = getLongFromArgN(context, 1, 20) ?: 200
        val loops = getBooleanFromArgN(context, 2) ?: true


        val image1 = BonkCommand::class.java.getResourceAsStream("/bonk1.png").use { ImageIO.read(it) }
        val image2 = BonkCommand::class.java.getResourceAsStream("/bonk2.png").use { ImageIO.read(it) }

        val graphics1 = image1.graphics
        val graphics2 = image2.graphics
        graphics1.drawImage(inputImg, 144, 144, 300, 300, null)
        graphics2.drawImage(inputImg, 60, 240, 420, 180, null)

        graphics1.dispose()
        graphics2.dispose()


        ByteArrayOutputStream().use {
            val gifEnc = GifEncoder(it, 498, 498, if (loops) 0 else 1)
            val options = ImageOptions()
                .setColorQuantizer(KMeansQuantizer.INSTANCE)
                .setDitherer(NearestColorDitherer.INSTANCE)
                .setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE)
                .setDelay(delay, TimeUnit.MILLISECONDS)

            val buffer = IntArray(248_004)
            gifEnc.addImage(image1.getRGB(0, 0, 498, 498, buffer, 0, 498), 498, options)
            options.setUsePreviousColors(true)
            gifEnc.addImage(image2.getRGB(0, 0, 498, 498, buffer, 0, 498), 498, options)

            gifEnc.finishEncoding()

            sendFileRsp(context, "**bonk** ${user.asTag} \uD83D\uDD28", it.toByteArray(), "gif")
        }
    }
}