package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import java.util.concurrent.TimeUnit

class PurgeCommand : AbstractCommand("command.purge") {

    private val silentPurgeName = "spurge"
    private val silentPruneName = "sprune"

    init {
        id = 39
        name = "purge"
        aliases = arrayOf(silentPurgeName, silentPruneName, "prune")
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        // + 1 is to start counting above the .purge command
        val amount = (getIntegerFromArgNMessage(context, 0, 1, 1000) ?: return) + 1
        val language = context.getLanguage()


        val messages = context.textChannel.iterableHistory.takeAsync(amount).await()
        for (message in messages) {
            context.container.purgedIds[message.idLong] = context.authorId
        }


        context.textChannel.purgeMessages(messages)
        val more = if (amount > 1) "more" else "one"
        val msg = i18n.getTranslation(language, "$root.success.$more")
            .replace("%amount%", amount.toString())

        if (context.commandParts[0].equals(silentPurgeName, true) || context.commandParts[0].equals(silentPruneName, true))
            sendMsg(context, msg)[0].delete().queueAfter(5, TimeUnit.SECONDS)
    }
}