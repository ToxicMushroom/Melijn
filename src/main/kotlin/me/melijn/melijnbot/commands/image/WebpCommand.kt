package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFile
import net.dv8tion.jda.api.Permission

class WebpCommand : AbstractCommand("command.webp") {

    init {
        name = "webp"
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.JPG, ImageType.TIFF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument + 0
        val lossy = context.optional(offset + 0, true) {
            getBooleanFromArgNMessage(context, it)
        } ?: return
        val compression = context.optional(offset + 1, 75) {
            getIntegerFromArgNMessage(context, it, 0, 100)
        } ?: return
        val losslessMode = context.optional(offset + 2, 6) {
            getIntegerFromArgNMessage(context, it, 0, 9)
        } ?: return
        val compressMode = context.optional(offset + 3, 4) {
            getIntegerFromArgNMessage(context, it, 0, 6)
        } ?: return

        val png = ImmutableImage.loader()
            .fromBytes(image.bytes)
            .bytes(WebpWriter(losslessMode, compression, compressMode, !lossy))

        val ogSize = StringUtils.humanReadableByteCountBin(image.bytes.size)
        val newSize = StringUtils.humanReadableByteCountBin(png.size)
        sendFile(
            context,
            "**$ogSize** -> **$newSize**",
            png, "webp"
        )
    }
}