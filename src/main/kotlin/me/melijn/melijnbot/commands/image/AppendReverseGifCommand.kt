package me.melijn.melijnbot.commands.image

import at.dhyan.open_imaging.GifDecoder
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.message.sendFile
import net.dv8tion.jda.api.Permission
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class AppendReverseGifCommand : AbstractCommand("command.appendreversegif") {

    init {
        id = 153
        name = "appendReverseGif"
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf()
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val gif = GifDecoder.read(image.bytes)
        val baos = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(baos).use { ios ->
            GifSequenceWriter(ios, gif.repetitions).use { gifWriter ->
                for (index in 0 until gif.frameCount)
                    gifWriter.writeToSequence(gif.getFrame(index), gif.getDelay(index) * 10)

                for (index in gif.frameCount - 1..0)
                    gifWriter.writeToSequence(gif.getFrame(index), gif.getDelay(index) * 10)
            }
        }

        val bytes = baos.toByteArray()
        sendFile(context, bytes, "gif")
    }
}