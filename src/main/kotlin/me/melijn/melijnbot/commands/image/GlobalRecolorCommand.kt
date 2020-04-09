package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.ImageUtils
import me.melijn.melijnbot.objects.utils.getColorFromArgNMessage
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.Color
import java.awt.image.BufferedImage

class GlobalRecolorCommand : AbstractCommand("command.globalrecolor") {

    init {
        id = 154
        name = "globalRecolor"
        aliases = arrayOf("globalRecolorGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("globalRecolorGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, argData ->
            val chosenColor = Color(argData.getInt("color"))
            ImageUtils.recolorPixelSingleOffset(image, 0) { ints: IntArray ->
                val c2 = ints[3] shl 24 or (chosenColor.red shl 16) or (chosenColor.green shl 8) or chosenColor.blue
                intArrayOf(c2 and 0xff, c2 shr 8 and 0xff, c2 shr 16 and 0xff, c2 shr 24 and 0xff)
            }

        }, argDataParser = { argInt: Int, argData: DataObject, _: DataObject ->
            val offset = getColorFromArgNMessage(context, argInt)
            if (offset == null) {
                false
            } else {
                argData.put("color", offset.rgb)
                true
            }

        }, imgDataParser = { _: BufferedImage, _: DataObject ->

        })
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, argData ->
            val chosenColor = Color(argData.getInt("color"))
            ImageUtils.recolorPixelSingleOffset(image, 0) { ints: IntArray ->
                val c2 = ints[3] shl 24 or (chosenColor.red shl 16) or (chosenColor.green shl 8) or chosenColor.blue
                val newColor = ImageUtils.suiteColorForGif(c2)

                intArrayOf(newColor and 0xff, newColor shr 8 and 0xff, newColor shr 16 and 0xff, newColor shr 24 and 0xff)
            }

        }, argDataParser = { argInt: Int, argData: DataObject, _: DataObject ->
            val offset = getColorFromArgNMessage(context, argInt)
            if (offset == null) {
                false
            } else {
                argData.put("color", offset.rgb)
                true
            }

        }, imgDataParser = { _: BufferedImage, _: DataObject ->

        }, argumentAmount = 1)
    }
}