package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.getIntegerFromArgN
import me.melijn.melijnbot.objects.utils.sendFile

class SpookifyCommand : AbstractCommand("command.spookify") {

    init {
        id = 55
        name = "spookify"
        aliases = arrayOf("spookifyGif")
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("spookifyGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        val pair = ImageUtils.getImageBytesNMessage(context) ?: return
        val imageByteArray = pair.first ?: return
        val argInt = if (pair.second) 1 else 0
        val offset = (getIntegerFromArgN(context, argInt + 0) ?: 128)

        val outputStream = ImageUtils.addEffectToStaticImage(imageByteArray) { image ->
            ImageUtils.recolorPixel(image, offset) { ints ->
                ImageUtils.getSpookyForPixel(ints[0], ints[1], ints[2], ints[3])
            }
        }
        sendFile(context, outputStream.toByteArray(), "png")
    }

    private suspend fun executeGif(context: CommandContext) {
        val pair = ImageUtils.getImageBytesNMessage(context) ?: return
        val imageByteArray = pair.first ?: return
        val argInt = if (pair.second) 1 else 0

        val quality = getIntegerFromArgN(context, argInt + 0) ?: 5
        val offset = (getIntegerFromArgN(context, argInt + 1) ?: 128)
        val fps = (getIntegerFromArgN(context, argInt + 2) ?: 20).toFloat()

        val outputStream = ImageUtils.addEffectToGifFrames(imageByteArray, fps, quality) { image ->
            ImageUtils.recolorPixel(image, offset) { ints ->
                ImageUtils.getSpookyForPixel(ints[0], ints[1], ints[2], ints[3])
            }
        }

        sendFile(context, outputStream.toByteArray(), "gif")
    }
}