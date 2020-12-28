package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil.defaultOffsetArgParser
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageUtils
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.image.BufferedImage
import java.lang.Integer.max

class BlurCommand : AbstractCommand("command.blur") {

    init {
        id = 135
        name = "blur"
        aliases = arrayOf("blurGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        cooldown = 5000
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.commandParts[1].equals("blurGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: ICommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, argData ->
            ImageUtils.blur(image, argData.getInt("offset"))

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            context.initCooldown()
            defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", max(img.height, img.width))
            imgData.put("defaultOffset", max(max(img.width, img.height) / 75, 1))

        })
    }

    private suspend fun executeGif(context: ICommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, argData ->
            ImageUtils.blur(image, argData.getInt("offset"), true)

        }, argDataParser = { argInt: Int, argData: DataObject, imgData: DataObject ->
            context.initCooldown()
            defaultOffsetArgParser(context, argInt, argData, imgData)

        }, imgDataParser = { img: BufferedImage, imgData: DataObject ->
            imgData.put("lower", 1)
            imgData.put("higher", max(img.height, img.width))
            imgData.put("defaultOffset", max(max(img.width, img.height) / 75, 1))

        }, argumentAmount = 1, debug = false)
    }
}