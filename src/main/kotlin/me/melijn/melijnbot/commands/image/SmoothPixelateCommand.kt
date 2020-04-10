package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.ImageUtils
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.image.BufferedImage

class SmoothPixelateCommand : AbstractCommand("command.smoothpixelate") {

    init {
        id = 134
        name = "smoothPixelate"
        aliases = arrayOf("smoothPixelateGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("smoothPixelateGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, imgData ->
            ImageUtils.smoothPixelate(image, imgData.getInt("offset"), true)

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            ImageCommandUtil.defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", Integer.max(img.height, img.width))
            imgData.put("defaultOffset", Integer.max(img.height, img.width) / 100)

        })
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, imgData ->
            ImageUtils.smoothPixelate(image, imgData.getInt("offset"), true)

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            ImageCommandUtil.defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", Integer.max(img.height, img.width))
            imgData.put("defaultOffset", Integer.max(img.height, img.width) / 100)

        })
    }
}