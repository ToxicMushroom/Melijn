package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getColorFromArgNMessage
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.Color
import java.awt.image.BufferedImage

class ReplaceColorCommand : AbstractCommand("command.replacecolor") {

    init {
        id = 173
        name = "replaceColor"
        aliases = arrayOf("rc", "replaceColorGif", "rcGif")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        if (context.commandParts[1].equals("replaceColorGif", true) || context.commandParts[1].equals("rcGif", true)) {
            executeGif(context)
        } else {
            executeNormal(context)
        }
    }

    private suspend fun executeNormal(context: CommandContext) {
        ImageCommandUtil.executeNormalEffect(context, effect = { image, argData ->
            val sourceColor = argData.getInt("color1")
            val newColor = Color(argData.getInt("color2"))
            val alpha1 = argData.getInt("alpha1")
            val alpha2 = argData.getInt("alpha2")

            ImageUtils.recolorPixelSingleOffset(image, 0) { ints: IntArray ->
                if ((alpha1 == -1 || ints[3] == alpha1) && (255 shl 24 or (ints[2] shl 16) or (ints[1] shl 8) or ints[0]) == sourceColor) {
                    val newAlpha = if (alpha2 == -1) ints[3] else alpha2
                    val c2 = newAlpha shl 24 or (newColor.red shl 16) or (newColor.green shl 8) or newColor.blue
                    intArrayOf(c2 and 0xff, c2 shr 8 and 0xff, c2 shr 16 and 0xff, c2 shr 24 and 0xff)
                } else {
                    ints
                }
            }
        }, argDataParser = argParser@{ argInt: Int, argData: DataObject, _: DataObject ->
            val sourceColor = getColorFromArgNMessage(context, argInt) ?: return@argParser false
            val newColor = getColorFromArgNMessage(context, argInt + 1) ?: return@argParser false

            val alpha1 = getIntegerFromArgN(context, argInt + 2) ?: -1
            val alpha2 = getIntegerFromArgN(context, argInt + 3) ?: -1

            argData.put("color1", sourceColor.rgb)
            argData.put("color2", newColor.rgb)
            argData.put("alpha1", alpha1)
            argData.put("alpha2", alpha2)
            return@argParser true

        }, imgDataParser = { _: BufferedImage, _: DataObject ->

        })
    }

    private suspend fun executeGif(context: CommandContext) {
        ImageCommandUtil.executeGifEffect(context, effect = { image, argData ->
            val sourceColor = argData.getInt("color1")
            val newColor = Color(argData.getInt("color2"))
            val alpha1 = argData.getInt("alpha1")
            val alpha2 = argData.getInt("alpha2")

            ImageUtils.recolorPixelSingleOffset(image, 0) { ints: IntArray ->
                if ((alpha1 == -1 || ints[3] == alpha1) && (255 shl 24 or (ints[2] shl 16) or (ints[1] shl 8) or ints[0]) == sourceColor) {
                    val newAlpha = if (alpha2 == -1) ints[3] else alpha2

                    val c2 = newAlpha shl 24 or (newColor.red shl 16) or (newColor.green shl 8) or newColor.blue
                    val newColor1 = ImageUtils.suiteColorForGif(c2)
                    intArrayOf(
                        newColor1 and 0xff,
                        newColor1 shr 8 and 0xff,
                        newColor1 shr 16 and 0xff,
                        newColor1 shr 24 and 0xff
                    )

                } else {
                    val c2 = ints[3] shl 24 or (ints[2] shl 16) or (ints[1] shl 8) or ints[0]
                    val newColor1 = ImageUtils.suiteColorForGif(c2)
                    intArrayOf(
                        newColor1 and 0xff,
                        newColor1 shr 8 and 0xff,
                        newColor1 shr 16 and 0xff,
                        newColor1 shr 24 and 0xff
                    )
                }
            }

        }, argDataParser = argParser@{ argInt: Int, argData: DataObject, _: DataObject ->
            val sourceColor = getColorFromArgNMessage(context, argInt) ?: return@argParser false
            val newColor = getColorFromArgNMessage(context, argInt + 1) ?: return@argParser false

            val alpha1 = getIntegerFromArgN(context, argInt + 2) ?: -1
            val alpha2 = getIntegerFromArgN(context, argInt + 3) ?: -1

            argData.put("color1", sourceColor.rgb)
            argData.put("color2", newColor.rgb)
            argData.put("alpha1", alpha1)
            argData.put("alpha2", alpha2)
            return@argParser true

        }, imgDataParser = { _: BufferedImage, _: DataObject ->

        }, argumentAmount = 1)
    }
}