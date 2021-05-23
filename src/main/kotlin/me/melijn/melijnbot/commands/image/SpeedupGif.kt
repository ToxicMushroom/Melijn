package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.enums.DiscordSize
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageType
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.getFloatFromArgNMessage
import net.dv8tion.jda.api.Permission
import kotlin.math.max

class SpeedupGif : AbstractCommand("command.speedupgif") {

    init {
        name = "speedupGif"
        aliases = arrayOf("sug")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }


    override suspend fun execute(context: ICommandContext) {
        val acceptTypes = setOf(ImageType.GIF)
        val image = ImageUtils.getImageBytesNMessage(context, 0, DiscordSize.X1024, acceptTypes) ?: return
        val multiplier = if (context.args.size > 1) getFloatFromArgNMessage(context, 1, 0.001f, 1000f) ?: return else 1f
        val unlock = if (context.args.size > 2) getBooleanFromArgNMessage(context, 2) ?: return else false
        val extension = if (unlock) "gif.removethis" else "gif"

        ImageCommandUtil.applyGifImmutableFrameModification(context, image, {}, { delay ->
            val value = (delay / multiplier).toInt()
            if (unlock) value
            else max(value, 2)
        }, extension = extension)
    }
}