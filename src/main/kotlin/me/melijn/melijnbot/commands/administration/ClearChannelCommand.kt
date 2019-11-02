package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.getTextChannelByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class ClearChannelCommand : AbstractCommand("command.clearchannel") {

    init {
        id = 40
        name = "clearChannel"
        aliases = arrayOf("cChannel")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        when {
            context.args[1] == "confirm" -> {
                val textChannel = getTextChannelByArgsNMessage(context, 0) ?: return
                textChannel.createCopy().await()
                textChannel.delete().queue()
            }
            context.args[0] == "confirm" -> {
                if (context.args.size > 1) {
                    sendSyntax(context)
                    return
                }
                val textChannel = context.getTextChannel()
                textChannel.createCopy().await()
                textChannel.delete().queue()
            }
            else -> {
                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.notconfirm")
                sendMsg(context, msg)
            }
        }
    }
}