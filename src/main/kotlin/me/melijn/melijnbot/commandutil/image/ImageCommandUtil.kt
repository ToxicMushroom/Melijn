package me.melijn.melijnbot.commandutil.image

import at.dhyan.open_imaging.GifDecoder
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.PngWriter
import me.melijn.melijnbot.commands.image.GifSequenceWriter
import me.melijn.melijnbot.commands.image.UserImageException
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import me.melijn.melijnbot.internals.utils.message.sendFile
import me.melijn.melijnbot.internals.utils.message.sendRsp
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

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
        val gif = GifDecoder.read(image.bytes)
        val baos = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(baos).use { ios ->
            GifSequenceWriter(ios, gif.repetitions).use { gifWriter ->
                for (index in 0 until gif.frameCount) {
                    val src = gif.getFrame(index)
                    val dst = ImmutableImage.wrapAwt(src).copy().awt()
                    modifications(src, dst)
                    gifWriter.writeToSequence(dst, gif.getDelay(index) * 10)
                }
            }
        }

        val bytes = baos.toByteArray()
        sendFile(context, bytes, "gif")
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
        if (message != null) sendFile(context, message, bytes, "png")
        else sendFile(context, bytes, "png")
    }

    suspend fun applyGifImmutableFrameModification(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modification: (img: ImmutableImage) -> Unit,
        delayFunction: ((current: Int) -> Int)? = null,
        message: String? = null,
        extension: String = "gif"
    ) {
        val gif = try {
            GifDecoder.read(image.bytes)
        } catch (t: IOException) {
            sendRsp(context, "Not a valid gif, (some tenor and giphy urls are actually mp4's)")
            return
        }
        val baos = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(baos).use { ios ->
            GifSequenceWriter(ios, gif.repetitions).use { gifWriter ->
                for (index in 0 until gif.frameCount) {
                    val src = ImmutableImage.wrapAwt(gif.getFrame(index))
                    modification(src)
                    val ogDelay = gif.getDelay(index)
                    val delay = delayFunction?.invoke(ogDelay) ?: ogDelay
                    gifWriter.writeToSequence(src.awt(), delay * 10)
                }
            }
        }

        val bytes = baos.toByteArray()
        if (message != null) sendFile(context, message, bytes, extension)
        else sendFile(context, bytes, extension)
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
        if (message != null) sendFile(context, message, bytes, "png")
        else sendFile(context, bytes, "png")
    }

    suspend fun createGifImmutableFrame(
        context: ICommandContext,
        image: ParsedImageByteArray,
        modification: (src: ImmutableImage) -> ImmutableImage,
        delayFunction: ((current: Int) -> Int)? = null,
        message: String? = null,
        extension: String = "gif"
    ) {
        val gif = GifDecoder.read(image.bytes)
        val baos = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(baos).use { ios ->
            GifSequenceWriter(ios, gif.repetitions).use { gifWriter ->
                for (index in 0 until gif.frameCount) {
                    val src = ImmutableImage.wrapAwt(gif.getFrame(index))
                    val new = try {
                        modification(src)
                    } catch (t: UserImageException) {
                        sendRsp(context, t.getUserFriendlyMessage())
                        return
                    }
                    val ogDelay = gif.getDelay(index)
                    val delay = delayFunction?.invoke(ogDelay) ?: ogDelay
                    gifWriter.writeToSequence(new.awt(), delay * 10)
                }
            }
        }

        val bytes = baos.toByteArray()
        if (message != null) sendFile(context, message, bytes, extension)
        else sendFile(context, bytes, extension)
    }
}