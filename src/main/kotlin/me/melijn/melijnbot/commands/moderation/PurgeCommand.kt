package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.await
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import java.util.concurrent.TimeUnit

class PurgeCommand : AbstractCommand("command.purge") {

    init {
        id = 39
        name = "purge"
        aliases = arrayOf("spurge")
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val amount = getIntegerFromArgNMessage(context, 0, 1, 1000) ?: return
        val language = context.getLanguage()
        val messages = context.getTextChannel().history.retrievePast(amount).await()

        context.getTextChannel().purgeMessages(messages)
        val msg = i18n.getTranslation(language, "$root.success")
            .replace("%amount%", amount.toString())


        sendMsg(context, msg)[0].delete().queueAfter(5, TimeUnit.SECONDS)
    }
}