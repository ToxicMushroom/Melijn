package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.commandutil.image.ImageCommandUtil.defaultOffsetArgParser
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
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
        val triple = ImageUtils.getImageBytesNMessage(context, "png") ?: return
        val blurred = ImageUtils.blur(context, triple.first, 9, false)

        sendFileRsp(context, blurred, "png")
    }

    private suspend fun executeGif(context: ICommandContext) {
        val triple = ImageUtils.getImageBytesNMessage(context, "gif") ?: return
        val blurReq = TaskManager.taskValueAsync { ImageUtils.blur(context, triple.first, 9, true) }

        //╯︿╰

        val loadingMsg = context.getTranslation("message.loading.effect")
        val lmsg = sendMsgAwaitEL(context, loadingMsg).firstOrNull()

        val blurred = blurReq.await()
        lmsg?.delete()?.queue()
        sendFileRsp(context, blurred, "gif")
    }
}