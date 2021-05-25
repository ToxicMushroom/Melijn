package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.ContrastFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission

class ContrastCommand : AbstractCommand("command.contrast") {

    init {
        name = "contrast"
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument + 0
        val intensity =
            (context.optional(offset, 200) { getIntegerFromArgNMessage(context, it, 1, 10000) } ?: return) / 100.0
        if (image.type == ImageType.GIF) {
            ImageCommandUtil.createGifImmutableFrame(context, image, contraster(intensity))
        } else {
            ImageCommandUtil.createImmutableImg(context, image, contraster(intensity))
        }
    }

    val contraster: (intensity: Double) -> ((src: ImmutableImage) -> ImmutableImage) = { intensity ->
        { src -> src.filter(ContrastFilter(intensity)) }
    }
}