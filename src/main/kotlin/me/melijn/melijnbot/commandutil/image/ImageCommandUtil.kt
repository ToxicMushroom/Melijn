package me.melijn.melijnbot.commandutil.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.AnimatedGif
import com.sksamuel.scrimage.nio.AnimatedGifReader
import com.sksamuel.scrimage.nio.ImageSource
import com.sksamuel.scrimage.nio.PngWriter
import me.melijn.melijnbot.commands.image.GifSequenceWriter
import me.melijn.melijnbot.commands.image.UserImageException
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.message.*
import net.dv8tion.jda.api.entities.Message
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

/** Gif/Image modification utilities with error handling, size exceptions ect **/
object ImageCommandUtil {

    suspend fun applyBufferedImgModification(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modifications: (src: BufferedImage, dst: BufferedImage) -> Unit
    ) {
        val immutableImg = ImmutableImage.loader().fromBytes(image.bytes)
        val src = immutableImg.awt()
        val dst = immutableImg.copy().awt()
        modifications(src, dst)

        val bytes = ImmutableImage.wrapAwt(dst).bytes(PngWriter(3))
        sendFile(context, bytes, "png")
    }

    suspend fun applyGifBufferFrameModification(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modifications: (src: BufferedImage, dst: BufferedImage) -> Unit
    ) {
        val bytes = writeModificationsToNewGifNMessage(context, image, -1) { index, gifWriter, gif ->
            val src = gif.getFrame(index).awt()
            val dst = gif.getFrame(index).copy().awt()
            modifications(src, dst)
            gifWriter.writeToSequence(dst, gif.getDelay(index).toMillis().toInt())
            true
        } ?: return

        sendFileRsp(context, bytes, "gif")
    }

    suspend fun applyImmutableImgModification(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modification: (img: ImmutableImage) -> Unit,
        message: String? = null
    ) {
        val immutableImg = ImmutableImage.loader().fromBytes(image.bytes)
        modification(immutableImg)

        val bytes = immutableImg.bytes(PngWriter(3))
        if (message != null) sendFileRsp(context, message, bytes, "png")
        else sendFileRsp(context, bytes, "png")
    }

    suspend fun applyGifImmutableFrameModification(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modification: (img: ImmutableImage) -> Unit,
        delayFunction: ((current: Int) -> Int)? = null,
        repeats: Int = -1,
        message: String? = null,
        extension: String = "gif"
    ) {
        val bytes = writeModificationsToNewGifNMessage(context, image, repeats) { index, gifWriter, gif ->
            val src = gif.getFrame(index)
            modification(src)
            val ogDelay = (gif.getDelay(index).toMillis() / 10).toInt()
            val delay = delayFunction?.invoke(ogDelay) ?: ogDelay
            gifWriter.writeToSequence(src.awt(), delay * 10)
            true
        } ?: return

        if (message != null) sendFileRsp(context, message, bytes, extension)
        else sendFileRsp(context, bytes, extension)
    }

    /**
     * GIF modification Helper function that handles, loading chat message while applying effects, checks size of resulting image for good error response.
     * Safely parses the gif, or send good error response
     * @param repeats | if -1 -> infinite, 0 -> no repeats, 1 -> 1 repeat ect. <-1 -> current gif repeat count
     * @param frameEditor | multi parameter function that should be used to write the modified gif frames to the gifWriter, return true if successful, return false to abort (make sure you send a message)
     * **/
    private suspend fun writeModificationsToNewGifNMessage(
        context: ICommandContext,
        image: ParsedImageByteArray,
        repeats: Int,
        frameEditor: suspend (index: Int, gifWriter: GifSequenceWriter, gif: AnimatedGif) -> Boolean
    ): ByteArray? {
        val message = sendRspAwaitEL(context, "<a:loading:867394725938331658> Applying image effects")
        fun deleteLoadingMessage(message: List<Message>) {
            message.firstOrNull()?.delete()?.reason("Temporary status progress message")?.queue()
        }
        try {
            val gif = getSafeGifImageNMessage(image, context) ?: return null
            val gifRepeats = when (repeats) {
                -1 -> gif.loopCount
                else -> repeats
            }
            val baos = ByteArrayOutputStream()
            ImageIO.createImageOutputStream(baos).use { ios ->
                GifSequenceWriter(ios, gifRepeats).use { gifWriter ->
                    for (index in 0 until gif.frameCount) {
                        val ok = frameEditor(index, gifWriter, gif)
                        if (!ok) {
                            deleteLoadingMessage(message)
                            return null
                        }
                    }
                }
            }

            val temp = baos.toByteArray()
            deleteLoadingMessage(message)
            if (temp.size > Message.MAX_FILE_SIZE) {
                sendMsgMaxFilesizeHit(context, image)
                return null
            }
            return temp
        } catch (t: Throwable) {
            t.sendInGuild(context)
        }
        deleteLoadingMessage(message)
        return null
    }


    suspend fun sendMsgMaxFilesizeHit(context: ICommandContext, image: ParsedImageByteArray) {
        val max = StringUtils.humanReadableByteCountBin(Message.MAX_FILE_SIZE)
        if (image.bytes.size > Message.MAX_FILE_SIZE) {
            sendRsp(context, "The source image ${image.url} is bigger then what I can send to discord (`${max}`)")
        } else {
            sendRsp(
                context,
                "The resulting image after applying effects is bigger then what I can send to discord (`${max}`) :/"
            )
        }
    }

    private suspend fun getSafeGifImageNMessage(
        image: ParsedImageByteArray,
        context: ICommandContext
    ): AnimatedGif? {
        return try {
            AnimatedGifReader.read(ImageSource.of(image.bytes))
        } catch (t: IOException) {
            sendRsp(context, "Not a valid gif")
            null
        }
    }

    suspend fun createImmutableImg(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modification: (src: ImmutableImage) -> ImmutableImage,
        message: String? = null
    ) {
        val immutableImg = ImmutableImage.loader().fromBytes(image.bytes)
        val new = try {
            modification(immutableImg)
        } catch (t: UserImageException) {
            sendRsp(context, t.getUserFriendlyMessage())
            return
        }

        val bytes = new.bytes(PngWriter(3))
        if (message != null) sendFileRsp(context, message, bytes, "png")
        else sendFileRsp(context, bytes, "png")
    }

    suspend fun createGifImmutableFrame(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modification: (src: ImmutableImage) -> ImmutableImage,
        delayFunction: ((current: Int) -> Int)? = null,
        message: String? = null,
        extension: String = "gif"
    ) {
        val bytes = writeModificationsToNewGifNMessage(context, image, -1) { index, gifWriter, gif ->
            val src = gif.getFrame(index)
            val new = try {
                modification(src)
            } catch (t: UserImageException) {
                sendRsp(context, t.getUserFriendlyMessage())
                return@writeModificationsToNewGifNMessage false
            }
            val ogDelay = gif.getDelay(index).toMillis().toInt() / 10
            val delay = delayFunction?.invoke(ogDelay) ?: ogDelay
            gifWriter.writeToSequence(new.awt(), delay * 10)
            true
        } ?: return
        if (message != null) sendFileRsp(context, message, bytes, extension)
        else sendFileRsp(context, bytes, extension)
    }
}