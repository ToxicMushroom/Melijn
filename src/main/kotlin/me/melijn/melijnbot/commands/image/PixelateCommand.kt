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
import java.lang.Integer.max

class PixelateCommand : AbstractCommand("command.pixelate") {

    init {
        id = 133
        name = "pixelate"
        aliases = arrayOf("pixelateGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.commandParts[1].equals("pixelateGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: ICommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, imgData ->
            ImageUtils.pixelate(image, imgData.getInt("offset"))

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            ImageCommandUtil.defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", max(img.height, img.width))
            imgData.put("defaultOffset", max(1, max(img.height, img.width) / 100))

        })

    }

    private suspend fun executeGif(context: ICommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, imgData ->
            ImageUtils.pixelate(image, imgData.getInt("offset"), true)

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            ImageCommandUtil.defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", max(img.height, img.width))
            imgData.put("defaultOffset", max(1, max(img.height, img.width) / 100))

        })
    }
}