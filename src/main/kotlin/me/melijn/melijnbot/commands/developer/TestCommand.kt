package me.melijn.melijnbot.commands.developer

import com.madgag.gif.fmsware.GifDecoder
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.sendFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.ImageIO


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
//        ffmPegFlushed(context)
    }

    private suspend fun ffmPegFlushed(context: CommandContext) {
        val triple = ImageUtils.getImageBytesNMessage(context) ?: return
        val image = triple.first
        val byteArrayInputStream = ByteArrayInputStream(image)
        val decoder = GifDecoder()
        decoder.read(byteArrayInputStream)


        val pb = ProcessBuilder("ffmpeg -f image2pipe -i - -f gif -".split(" "))
        val p = pb.start()
        for (i in 0 until decoder.frameCount) {
            //val pb = ProcessBuilder("ffmpeg.exe -f image2 -framerate 24 -i 0%03d.png -vf scale=256x256 boo.mp4".split(" "))


            val frame1 = decoder.getFrame(i)
            ImageIO.write(frame1, "png", p.outputStream)
        }
        p.outputStream.close()


        val byteArray = p.inputStream.readAllBytes()


        p.inputStream.close()
        sendFile(context, byteArray, "png")
    }


    private fun toByteArray(`in`: InputStream): ByteArray? {
        val os = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var len: Int

        // read bytes from the input stream and store them in buffer
        while (`in`.read(buffer).also { len = it } != -1) {
            // write bytes from the buffer into output stream
            os.write(buffer, 0, len)
        }
        return os.toByteArray()
    }
}