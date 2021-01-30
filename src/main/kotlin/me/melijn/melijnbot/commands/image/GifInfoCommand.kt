package me.melijn.melijnbot.commands.image

import com.madgag.gif.fmsware.GifDecoder
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.toHex
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.utils.data.DataArray
import java.awt.Color
import java.io.ByteArrayInputStream


class GifInfoCommand : AbstractCommand("command.gifinfo") {

    init {
        id = 131
        name = "gifInfo"
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val triple = ImageUtils.getImageBytesNMessage(context) ?: return
        val image = triple.first
        val byteArrayInputStream = ByteArrayInputStream(image)
        val decoder = GifDecoder()
        decoder.read(byteArrayInputStream)

        val gifInfoTitle = context.getTranslation("$root.eb.gif.title")

        val eb = Embedder(context)
            .setThumbnail(triple.second)

        val loops = if (decoder.loopCount == 0) {
            "∞"
        } else {
            "${decoder.loopCount}"
        }
        val frames = decoder.frameCount
        val width = decoder.frameSize.width
        val height = decoder.frameSize.height
        val gct = decoder.gct ?: emptyArray<Int>().toIntArray()


        val globalDataArray = DataArray.empty()
        for (i in gct) {
            val color = Color(i)
            globalDataArray.add(color.toHex())
        }

        val globalUrl = if (gct.isEmpty()) {
            null
        } else {
            context.webManager.binApis.postToHastebin("json", globalDataArray.toString())
        }

        val gifInfoValue = context.getTranslation("$root.eb.gif.value")
            .withVariable("frames", "$frames")
            .withVariable("loops", loops)
            .withVariable("width", "$width")
            .withVariable("height", "$height")
            .withVariable("gctUrl", globalUrl ?: "/")



        eb.addField(gifInfoTitle, gifInfoValue, false)

        val frame = getIntegerFromArgN(context, if (triple.third) 1 else 0, 0, frames - 1)
        if (frame != null) {
            val frameMeta = decoder.getFrameMeta(frame)
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

            val da = DataArray.empty()
            for (i in lct) {
                val color = Color(i)
                da.add(color.toHex())
            }

            val url = if (lct.isEmpty()) {
                null
            } else {
                context.webManager.binApis.postToHastebin("json", da.toString())
            }

            val frameInfoTitle = context.getTranslation("$root.eb.frame.title")
            val frameInfoValue = context.getTranslation("$root.eb.frame.value")
                .withVariable("delay", frameMeta.delay.toString())
                .withVariable("transColor", transColor?.toHex() ?: "/")
                .withVariable("bgColor", bgColor?.toHex() ?: "/")
                .withVariable("lctUrl", url ?: "/")

            eb.addField(frameInfoTitle, frameInfoValue, false)
        }

        sendEmbedRsp(context, eb.build())
    }
}