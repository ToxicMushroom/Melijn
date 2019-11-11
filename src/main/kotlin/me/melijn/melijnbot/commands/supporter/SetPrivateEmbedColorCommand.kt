package me.melijn.melijnbot.commands.supporter

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.getColorFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import okhttp3.internal.toHexString

class SetPrivateEmbedColorCommand : AbstractCommand("command.setprivateembedcolor") {

    init {
        id = 76
        name = "setPrivateEmbedColor"
        aliases = arrayOf("spec")
        commandCategory = CommandCategory.SUPPORTER
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.userEmbedColorWrapper
        val msg = if (context.args.isEmpty()) {
            val colorInt = wrapper.userEmbedColorCache.get(context.authorId).await()

            if (colorInt == -1) {
                context.getTranslation("$root.show.unset")
            } else {
                context.getTranslation("$root.show.set")
                    .replace("%color%", colorInt.toHexString())
            }
        } else {
            if (context.rawArg.equals("null", true)) {
                wrapper.removeColor(context.authorId)

                context.getTranslation("$root.unset")
            } else {
                val color = getColorFromArgNMessage(context, 0) ?: return
                wrapper.setColor(context.authorId, color.rgb)

                context.getTranslation("$root.set")
                    .replace(PLACEHOLDER_ARG, color.rgb.toHexString())
            }
        }
        sendMsg(context, msg)
    }
}