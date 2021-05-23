package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import net.dv8tion.jda.api.Permission

class RotateLeftCommand : AbstractCommand("command.rotateleft") {

    init {
        name = "rotateLeft"
        aliases = arrayOf("rotatel")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        if (image.type == ImageType.GIF) ImageCommandUtil.createGifImmutableFrame(context, image, modification)
        else ImageCommandUtil.createImmutableImg(context, image, modification)
    }

    private val modification:((src: ImmutableImage) -> ImmutableImage) = { src -> src.rotateLeft() }
}