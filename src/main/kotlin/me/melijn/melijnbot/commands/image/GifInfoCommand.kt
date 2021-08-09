package me.melijn.melijnbot.commands.image

import at.dhyan.open_imaging.GifDecoder
import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp

class GifInfoCommand : AbstractCommand("command.gifinfo") {

    init {
        id = 131
        name = "gifInfo"
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument.plus(0)
        val gif = GifDecoder.read(image.bytes)
        val colorDepth = gif.colorResolution
        val repetitions = gif.repetitions.let { if (it == 0) "infinite" else it.toString() }
        val frames = gif.frameCount
        val height = gif.height
        val width = gif.width
        val index = context.optional(offset, 0) { getIntegerFromArgNMessage(context, 1, 0, frames - 1) } ?: return
        val delay = gif.getDelay(index)
        val transparency = ImmutableImage.wrapAwt(gif.getFrame(index)).hasTransparency()

        val eb = Embedder(context)
            .setDescription(
                """
                    |```INI
                    |[Gif Info]``````LDIF
                    |Width x Height: $width x $height
                    |Frame Count: $frames
                    |Repetitions: $repetitions
                    |Color Depth: $colorDepth
                    |Header: ${gif.header}
                    |App: ${gif.appAuthCode} ${gif.appId}
                    |```
                    |```INI
                    |[Frame #$index]``````LDIF
                    |Delay: $delay centi-seconds
                    |Has Transparency: $transparency
                    |```
                """.trimMargin()
            )
            .setThumbnail(image.url)
        sendEmbedRsp(context, eb.build())
    }
}