package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.UserFriendlyException
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission

class TrimCommand : AbstractCommand("command.trim") {

    init {
        name = "trim"
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return

        val side = getEnumFromArgNMessage<Sides>(context, image.usedArgument + 0, "message.unknown.side") ?: return
        val amount = getIntegerFromArgNMessage(context, image.usedArgument + 1) ?: return
        if (image.type == ImageType.GIF) ImageCommandUtil.createGifImmutableFrame(context, image, modification(side, amount))
        else ImageCommandUtil.createImmutableImg(context, image, modification(side, amount))
    }

    private val modification: (sides: Sides, amount: Int) -> ((src: ImmutableImage) -> ImmutableImage) = { side, amount ->
        { src ->
            val top = side.top * amount
            val right = side.right * amount
            val bottom = side.bottom * amount
            val left = side.left * amount
            val trimWidth = right + left
            val trimHeight = bottom + top
            if (trimWidth >= src.width) {
                throw UserImageException("You tried trimming **$trimWidth** horizontally but the image is only **${src.width} x ${src.height}**")
            }
            if (trimHeight >= src.height) {
                throw UserImageException("You tried trimming **$trimWidth** vertically but the image is only **${src.width} x ${src.height}**")
            }

            src.trim(left, top, right, bottom)
        }
    }
}
class UserImageException(override val message: String) : UserFriendlyException(message) {
    override fun getUserFriendlyMessage(): String {
        return message
    }
}

enum class Sides(val top: Int, val right: Int, val bottom: Int, val left: Int) {
    TOP(1, 0, 0, 0),
    RIGHT(0, 1, 0, 0),
    BOTTOM(0, 0, 1, 0),
    LEFT(0, 0, 0, 1),
    HORIZONTAL(0, 1, 0, 1),
    VERTICAL(1, 0, 1, 0),
    ALL(1, 1, 1, 1)
}