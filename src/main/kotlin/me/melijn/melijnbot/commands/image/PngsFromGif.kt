package me.melijn.melijnbot.commands.image

import com.madgag.gif.fmsware.GifDecoder
import me.melijn.melijnbot.commands.utility.prependZeros
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.sendFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry

import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO


class PngsFromGif : AbstractCommand("command.pngsfromgif") {

    init {
        id = 170
        name = "pngsFromGif"
        aliases = arrayOf("pfg")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        val triple = ImageUtils.getImageBytesNMessage(context, "gif") ?: return
        val decoder = GifDecoder()
        val inputStream = ByteArrayInputStream(triple.first)
        decoder.read(inputStream)

        val byteArrayOutputStream = ByteArrayOutputStream()
        val zipOutputStream = ZipOutputStream(byteArrayOutputStream)

        zipOutputStream.use { zos ->
            for (i in 0 until decoder.frameCount) {
                val coolFrame = decoder.getFrame(i)
                val zipEntry = ZipEntry("frame_${gitGud(i, decoder.frameCount)}.png")
                zos.putNextEntry(zipEntry)
                val baos = ByteArrayOutputStream()
                ImageIO.write(coolFrame, "png", baos)
                baos.flush()
                val imageInByte = baos.toByteArray()
                baos.close()
                zos.write(imageInByte)
                zos.closeEntry()
            }
        }

        sendFile(context, byteArrayOutputStream.toByteArray(), "zip")
        // zipOutputStream
    }

    private fun gitGud(cool: Int, maxSize: Int): String {
        val shouldSize = maxSize.toString().length + 1
        return cool.toString().prependZeros(shouldSize)
    }
}