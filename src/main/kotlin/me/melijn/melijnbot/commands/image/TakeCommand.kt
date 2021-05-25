package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission

class TakeCommand : AbstractCommand("command.take") {

    init {
        name = "take"
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val side = getEnumFromArgNMessage<Sides>(context, image.usedArgument + 0, "message.unknown.side") ?: return
        val amount = getIntegerFromArgNMessage(context, image.usedArgument + 1, 1) ?: return
        if (image.type == ImageType.GIF) ImageCommandUtil.createGifImmutableFrame(
            context,
            image,
            modification(side, amount)
        )
        else ImageCommandUtil.createImmutableImg(context, image, modification(side, amount))
    }

    private val modification: (sides: Sides, amount: Int) -> ((src: ImmutableImage) -> ImmutableImage) =
        { side, amount ->
            { src ->
                val top = side.top * amount
                val right = side.right * amount
                val bottom = side.bottom * amount
                val left = side.left * amount
                var dest = if (top > 0) src.takeTop(top) else src
                dest = if (right > 0) dest.takeRight(right) else dest
                dest = if (bottom > 0) dest.takeBottom(bottom) else dest
                dest = if (left > 0) dest.takeLeft(left) else dest
                dest
            }
        }
}
