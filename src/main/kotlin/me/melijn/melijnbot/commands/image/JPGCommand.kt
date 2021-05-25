package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFile
import net.dv8tion.jda.api.Permission

class JPGCommand : AbstractCommand("command.jpg") {

    init {
        name = "jpg"
        aliases = arrayOf("jpeg")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.JPG, ImageType.TIFF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument
        val quality = context.optional(offset + 0, 100) { getIntegerFromArgNMessage(context, it, 0, 100) } ?: return
        val progressive = context.optional(offset + 1, false) { getBooleanFromArgNMessage(context, it) } ?: return
        val jpeg = ImmutableImage.loader()
            .fromBytes(image.bytes)
            .bytes(JpegWriter(quality, progressive))

        val ogSize = StringUtils.humanReadableByteCountBin(image.bytes.size)
        val newSize = StringUtils.humanReadableByteCountBin(jpeg.size)
        sendFile(
            context,
            "**$ogSize** -> **$newSize**",
            jpeg, "jpg"
        )
    }
}