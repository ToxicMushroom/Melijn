package me.melijn.melijnbot.commandutil.image

import com.madgag.gif.fmsware.GifDecoder
import com.squareup.gifencoder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.min

object ImageCommandUtil {


    // needs lower and higher to be put in the imgData with imgParser arg
    suspend fun defaultOffsetArgParser(context: CommandContext, argInt: Int, argData: DataObject, imgData: DataObject): Boolean {
        val lower = imgData.getInt("lower")
        val higher = imgData.getInt("higher")
        return if (argInt == context.args.size) {
            argData.put("offset", imgData.getInt("defaultOffset"))
            true
        } else {
            val offset = getIntegerFromArgNMessage(context, argInt, lower, higher)
            if (offset == null) {
                false
            } else {
                argData.put("offset", offset)
                true
            }
        }
    }


    // arg data must contain "offset" -> Int (-255 -> 255)
    suspend fun executeNormalRecolorSingleOffset(
        context: CommandContext,
        recolor: (ints: IntArray) -> IntArray
    ) {
        executeNormalEffect(context, { image, argData ->
            ImageUtils.recolorPixelSingleOffset(image, argData.getInt("offset")) { ints ->
                recolor(ints)
            }
        }, { argInt: Int, argData: DataObject, _: DataObject ->
            if (argInt == context.args.size) {
                argData.put("offset", 128)
                true
            } else {
                val offset = getIntegerFromArgNMessage(context, argInt, -255, 255)
                if (offset == null) {
                    false
                } else {
                    argData.put("offset", offset)
                    true
                }
            }
        }, { _: BufferedImage, _: DataObject ->

        })
    }

    // arg data must contain "offset" -> Int (-255 -> 255)
    suspend fun executeGifRecolorSingleOffset(
        context: CommandContext,
        recolor: (ints: IntArray) -> IntArray, // ints: r, g, b, offset (def: 255)
        debug: Boolean = false
    ) {
        executeGifEffect(context, { image, argData ->
            val offset = max(-255, min(argData.getInt("offset"), 255))
            ImageUtils.recolorPixelSingleOffset(image, offset) { ints ->
                recolor(ints)
            }
        }, { argInt: Int, argData: DataObject, _: DataObject ->
            if (argInt == context.args.size) {
                argData.put("offset", 128)
                true
            } else {
                val offset = getIntegerFromArgNMessage(context, argInt, -255, 255)
                if (offset == null) {
                    false
                } else {
                    argData.put("offset", offset)
                    true
                }
            }
        }, { _: BufferedImage, _: DataObject ->

        }, 1, debug)
    }


    // EFFECTS
    suspend fun executeNormalEffect(
        context: CommandContext,
        effect: (image: BufferedImage, argData: DataObject) -> Unit,
        argDataParser: suspend (argInt: Int, argData: DataObject, imgData: DataObject) -> Boolean = { _: Int, _: DataObject, _: DataObject ->
            true
        },
        imgDataParser: (img: BufferedImage, imgData: DataObject) -> Unit = { _: BufferedImage, _: DataObject -> }
    ) {
        executeNormalTransform(context, { byteArray, argData ->
            ImageUtils.addEffectToStaticImage(byteArray) { image ->
                effect(image, argData)
            }
        }, argDataParser, imgDataParser)
    }

    suspend fun executeGifEffect(
        context: CommandContext,
        effect: (image: BufferedImage, argData: DataObject) -> Unit,
        argDataParser: suspend (argInt: Int, argData: DataObject, imgData: DataObject) -> Boolean = { _: Int, _: DataObject, _: DataObject ->
            true
        },
        imgDataParser: (img: BufferedImage, imgData: DataObject) -> Unit = { _: BufferedImage, _: DataObject -> },
        argumentAmount: Int = 0,
        debug: Boolean = false
    ) {
        executeGifTransform(context, { gifDecoder, fps, repeat, argData ->
            if (debug) ImageUtils.addEffectToGifFrames(gifDecoder, fps, repeat, { image ->
                effect(image, argData)
            }, context)
            else ImageUtils.addEffectToGifFrames(gifDecoder, fps, repeat, { image ->
                effect(image, argData)
            })

        }, argDataParser, imgDataParser, argumentAmount)
    }


    // FRAME APPENDER THING
    suspend fun executeGifFrameAppend(
        context: CommandContext,
        debug: Boolean = false
    ) {
        executeGifTransform(context, { gifDecoder, fps, repeat, _ ->
            val outputStream = ByteArrayOutputStream()
            val repeatCount = if (repeat != null && repeat == true) {
                0
            } else if (repeat != null && repeat == false) {
                -1
            } else {
                gifDecoder.loopCount
            }

            val width = gifDecoder.getFrame(0).width
            val height = gifDecoder.getFrame(0).height
            val encoder = GifEncoder(outputStream, width, height, repeatCount)


            val gct = gifDecoder.gct ?: emptyArray<Int>().toIntArray()

            for (index in 0 until gifDecoder.frameCount) {
                addFrameToEncoderReverseThingie(gifDecoder, debug, gct, index, width, height, encoder, context, fps, false)
            }

            for (fakeIndex in 0 until (gifDecoder.frameCount - 1)) {
                val index = gifDecoder.frameCount - fakeIndex - 2
                addFrameToEncoderReverseThingie(gifDecoder, debug, gct, index, width, height, encoder, context, fps, true)
            }

            encoder.finishEncoding()

            outputStream
        }, { _: Int, _: DataObject, _: DataObject ->
            true
        }, { _: BufferedImage, _: DataObject ->

        }, 0)
    }

    private fun addFrameToEncoderReverseThingie(decoder: GifDecoder, debug: Boolean, gct: IntArray, index: Int, width: Int, height: Int, encoder: GifEncoder, context: CommandContext, fps: Float?, isSecondIteration: Boolean) {
        val options = ImageOptions()
        options.setColorQuantizer(MedianCutQuantizer.INSTANCE)
        options.setDitherer(FloydSteinbergDitherer.INSTANCE)
        options.setTransparencyColor(Color.WHITE.rgb)
        options.setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE)


        val frameMeta = decoder.getFrameMeta(index)
        val gifFrame = frameMeta.image

        if (!isSecondIteration) ImageUtils.recolorPixelSingleOffset(gifFrame, colorPicker = { ints ->
            if (ints[0] == 255 && ints[1] == 255 && ints[2] == 255) {
                intArrayOf(254, 254, 254, 255)
            } else if (ints[3] < 128) {
                intArrayOf(255, 255, 255, 255)
            } else ints
        })

        val delay = fps?.let { (1.0 / it * 1000.0).toLong() } ?: frameMeta.delay.toLong()
        options.setDelay(delay, TimeUnit.MILLISECONDS)

        if (debug) {
            runBlocking {
                val lct = if (frameMeta.lct.isEmpty()) {
                    gct
                } else {
                    frameMeta.lct
                }

                val bgColor = if (lct.size > frameMeta.bgIndex && frameMeta.bgIndex != -1) {
                    Color(lct[frameMeta.bgIndex])
                } else {
                    null
                }

                val transColor = if (lct.size > frameMeta.transIndex && frameMeta.transIndex != -1) {
                    Color(lct[frameMeta.transIndex])
                } else {
                    null
                }

                sendMsgAwaitEL(context, "bg: $bgColor, trans: $transColor", gifFrame, "gif")
            }
        }

        encoder.addImage(
            gifFrame.getRGB(0, 0, width, height, Array(width * height) { 0 }.toIntArray(), 0, width),
            width, options)
    }


    // ROOT TRANSFORM THING
    private suspend fun executeNormalTransform(
        context: CommandContext,
        transform: (byteArray: ByteArray, argData: DataObject) -> ByteArrayOutputStream,
        argDataParser: suspend (argInt: Int, argData: DataObject, imgData: DataObject) -> Boolean = { _: Int, _: DataObject, _: DataObject ->
            true
        },
        imgDataParser: (img: BufferedImage, imgData: DataObject) -> Unit = { _: BufferedImage, _: DataObject -> }
    ) {
        val triple = ImageUtils.getImageBytesNMessage(context) ?: return
        val imageByteArray = triple.first
        val argInt = if (triple.third) 1 else 0

        val loadingMsg = context.getTranslation("message.loading.effect")
        val lmsg = sendMsgAwaitEL(context, loadingMsg).firstOrNull()

        val img = withContext(Dispatchers.IO) {
            ImageIO.read(ByteArrayInputStream(triple.first))
        }

        val imgData = DataObject.empty()
        imgDataParser(img, imgData)

        val argData = DataObject.empty()
        if (!argDataParser(argInt, argData, imgData)) { // The arg data parser will send the error message and return false
            return
        }

        val outputStream = transform(imageByteArray, argData)
        sendFile(context, outputStream.toByteArray(), "png")
        lmsg?.delete()?.queue()
    }


    private suspend fun executeGifTransform(
        context: CommandContext,
        transform: (decoder: GifDecoder, fps: Float?, repeat: Boolean?, argData: DataObject) -> ByteArrayOutputStream,
        argDataParser: suspend (argInt: Int, argData: DataObject, imgData: DataObject) -> Boolean = { _: Int, _: DataObject, _: DataObject ->
            true
        },
        imgDataParser: (img: BufferedImage, imgData: DataObject) -> Unit = { _: BufferedImage, _: DataObject -> },
        argumentAmount: Int = 0
    ) {
        val triple = ImageUtils.getImageBytesNMessage(context, "gif") ?: return
        val argInt = if (triple.third) 1 else 0

        //╯︿╰

        val loadingMsg = context.getTranslation("message.loading.effect")
        val lmsg = sendMsgAwaitEL(context, loadingMsg).firstOrNull()


        val decoder = GifDecoder()
        val inputStream = ByteArrayInputStream(triple.first)
        decoder.read(inputStream)
        val img = decoder.image

        val imgData = DataObject.empty()
        imgDataParser(img, imgData)

        val argData = DataObject.empty()
        if (!argDataParser(argInt, argData, imgData)) { // The arg data parser will send the error message and return false
            return
        }

        val repeat = getBooleanFromArgN(context, argInt + argumentAmount)
        val fps = getIntegerFromArgN(context, argInt + argumentAmount)?.toFloat()

        val outputStream = transform(decoder, fps, repeat, argData)
        sendFile(context, outputStream.toByteArray(), "gif")
        lmsg?.delete()?.queue()
    }
}