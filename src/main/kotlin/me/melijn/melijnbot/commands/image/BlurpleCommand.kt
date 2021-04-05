package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.ImageUtils
import net.dv8tion.jda.api.Permission


class BlurpleCommand : AbstractCommand("command.blurple") {

    init {
        id = 54
        name = "blurple"
        aliases = arrayOf("blurpleGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    suspend fun execute(context: ICommandContext) {
        if (context.commandParts[1].equals("blurpleGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: ICommandContext) {
        ImageCommandUtil.executeNormalRecolorSingleOffset(context) { ints ->
            ImageUtils.getBlurpleForPixel(ints[0], ints[1], ints[2], ints[3], ints[4])
        }
    }

    private suspend fun executeGif(context: ICommandContext) {
        ImageCommandUtil.executeGifRecolorSingleOffset(context, { ints ->
            ImageUtils.getBlurpleForPixel(ints[0], ints[1], ints[2], ints[3], ints[4], true)
        }, false)
    }
}
