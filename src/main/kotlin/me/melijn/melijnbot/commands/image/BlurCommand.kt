package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.ImageUtils
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendFileRsp
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import net.dv8tion.jda.api.Permission

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
        val triple = ImageUtils.getImageBytesNMessage(context) ?: return
        val offset = if (triple.third) 1 else 0
        val radius = if (context.args.size > offset) {
            getIntegerFromArgNMessage(context, offset, 1, 9) ?: return
        } else {
            3
        }

        val repeats = if (context.args.size > offset + 1) {
            getIntegerFromArgNMessage(context, offset + 1, -1, 1000) ?: return
        } else {
            -1
        }

        val delayCentiseconds = if (context.args.size > offset + 2) {
            getIntegerFromArgNMessage(context, offset + 2, 0, 65536) ?: return
        } else {
            0
        }

        val blurReq = TaskManager.taskValueAsync {
            ImageUtils.blur(context, triple.first, radius, repeats, delayCentiseconds)
        }

        //╯︿╰

        val loadingMsg = context.getTranslation("message.loading.effect")
        val lmsg = sendMsgAwaitEL(context, loadingMsg).firstOrNull()

        val blurred = blurReq.await()
        lmsg?.delete()?.queue()
        sendFileRsp(context, blurred, "gif")
    }
}