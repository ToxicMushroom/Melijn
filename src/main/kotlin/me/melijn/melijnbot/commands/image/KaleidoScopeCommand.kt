package me.melijn.melijnbot.commands.image

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.filter.KaleidoscopeFilter
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission

class KaleidoScopeCommand : AbstractCommand("command.kaleidoscope") {

    init {
        name = "kaleidoScope"
        aliases = arrayOf("kaleido", "kScope")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf()
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.PNG, ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val offset = image.usedArgument + 0
        val intensity =
            (context.optional(offset, 3) { getIntegerFromArgNMessage(context, it, 3, 2000) } ?: return)
        if (image.type == ImageType.GIF) {
            ImageCommandUtil.createGifImmutableFrame(context, image, scoper(intensity))
        } else {
            ImageCommandUtil.createImmutableImg(context, image, scoper(intensity))
        }
    }

    val scoper: (sides: Int) -> ((src: ImmutableImage) -> ImmutableImage) = { sides ->
        { src -> src.filter(KaleidoscopeFilter(sides)) }
    }
}