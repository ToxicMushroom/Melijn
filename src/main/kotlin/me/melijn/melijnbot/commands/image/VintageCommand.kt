package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.filter.VintageFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.ParsedImageByteArray
import net.dv8tion.jda.api.Permission

class VintageCommand : AbstractCommand("command.vintage") {

    init {
        name = "vintage"
        aliases = arrayOf("vintageGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf()
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) {
            vintageGif(context, image)
        } else {
            vintageNormal(context, image)
        }
    }

    private suspend fun vintageNormal(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyImmutableImgModification(context, image, { img ->
            VintageFilter().apply(img)
        })
    }

    private suspend fun vintageGif(context: ICommandContext, image: ParsedImageByteArray) {
        ImageCommandUtil.applyGifImmutableFrameModification(context, image, { img ->
            VintageFilter().apply(img)
        })
    }
}