package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageUtils
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

    override suspend fun execute(context: ICommandContext) {
        if (context.commandParts[1].equals("smoothPixelateGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: ICommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, imgData ->
            ImageUtils.smoothPixelate(image, imgData.getInt("offset"))

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            ImageCommandUtil.defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", Integer.max(img.height, img.width))
            imgData.put("defaultOffset", Integer.max(img.height, img.width) / 100)

        })
    }

    private suspend fun executeGif(context: ICommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, imgData ->
            ImageUtils.smoothPixelate(image, imgData.getInt("offset"))

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            ImageCommandUtil.defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", Integer.max(img.height, img.width))
            imgData.put("defaultOffset", Integer.max(img.height, img.width) / 100)

        })
    }
}