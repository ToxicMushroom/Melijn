package me.melijn.melijnbot.commands.image

import at.dhyan.open_imaging.GifDecoder
import me.melijn.melijnbot.commands.utility.prependZeros
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO

class PngsFromGifCommand : AbstractCommand("command.pngsfromgif") {

    init {
        id = 170
        name = "pngsFromGif"
        aliases = arrayOf("pfg")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val gif = GifDecoder.read(image.bytes)

        ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                for (i in 0 until gif.frameCount) {
                    val coolFrame = gif.getFrame(i)
                    val zipEntry = ZipEntry("frame_${gitGud(i, gif.frameCount)}.png")

                    zos.putNextEntry(zipEntry)

                    ByteArrayOutputStream().use { baos2 ->
                        ImageIO.write(coolFrame, "png", baos2)
                        baos2.flush()
                        val imageInByte = baos2.toByteArray()
                        zos.write(imageInByte)
                    }
                }
            }

            sendFileRsp(context, baos.toByteArray(), "zip")
        }
    }

    private fun gitGud(cool: Int, maxSize: Int): String {
        val shouldSize = maxSize.toString().length + 1
        return cool.toString().prependZeros(shouldSize)
    }
}