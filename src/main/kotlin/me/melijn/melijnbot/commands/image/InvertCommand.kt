package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission

class InvertCommand : AbstractCommand("command.invert") {


    init {
        id = 56
        name = "invert"
        aliases = arrayOf("invertGif")
        discordPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("invertGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalRecolor(context, { ints ->
            ImageUtils.getInvertedPixel(ints[0], ints[1], ints[2])
        }, false)
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifRecolor(context, { ints ->
            ImageUtils.getInvertedPixel(ints[0], ints[1], ints[2])
        }, false)
    }
}