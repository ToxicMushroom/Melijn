package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission
import java.lang.Integer.max

class BlurCommand : AbstractCommand("command.blur") {

    init {
        id = 135
        name = "blur"
        aliases = arrayOf("blurGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("blurGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, i ->
            val timeStamp = System.currentTimeMillis()
            ImageUtils.blur(image, i)
            println(System.currentTimeMillis() - timeStamp)


        }, hasOffset = true, defaultOffset = {
            max(max(it.width, it.height) / 75, 1)

        }, offsetRange = { img ->
            IntRange(1, max(img.height, img.width))

        })
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, i ->
            val timeStamp = System.currentTimeMillis()
            ImageUtils.blur(image, i)
            println(System.currentTimeMillis() - timeStamp)

        }, hasOffset = true, defaultOffset = {
            max(max(it.width, it.height) / 75, 1)

        }, offsetRange = { img ->
            IntRange(1, max(img.height, img.width))

        })
    }
}