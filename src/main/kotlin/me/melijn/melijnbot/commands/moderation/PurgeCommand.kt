package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import java.util.concurrent.TimeUnit

class PurgeCommand : AbstractCommand("command.purge") {

    private val silentPurgeName = "spurge"
    private val silentPruneName = "sprune"

    init {
        id = 39
        name = "purge"
        aliases = arrayOf(silentPurgeName, silentPruneName, "prune")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        // + 1 is to start counting above the .purge command
        val amount = (getIntegerFromArgNMessage(context, 0, 1, 1000) ?: return) + 1

        val targetUser = if (context.args.size == 2) {
            retrieveUserByArgsNMessage(context, 1) ?: return
        } else {
            null
        }

        val messages = mutableListOf<Message>()
        var counter = 1
        context.textChannel.iterableHistory
            .forEachAsync { message ->
                if (targetUser == null) {
                    messages.add(message)
                } else {
                    if (message.author.idLong == targetUser.idLong) {
                        messages.add(message)
                    }
                }
                if (amount <= counter) {
                    false
                } else {
                    counter++
                    true
                }
            }
            .thenRun {
                context.taskManager.async {
                    for (message in messages) {
                        context.container.purgedIds[message.idLong] = context.authorId
                    }


                    context.textChannel.purgeMessages(messages)
                    val userMore = if (targetUser == null) "" else "user"
                    val more = if (amount > 1) "more" else "one"
                    val msg = context.getTranslation("$root.success.$userMore$more")
                        .replace("%amount%", amount.toString())
                        .replace(PLACEHOLDER_USER, targetUser?.asTag ?: "")

                    if (context.commandParts[0].equals(silentPurgeName, true) || context.commandParts[0].equals(silentPruneName, true))
                        sendMsg(context, msg)[0].delete().queueAfter(5, TimeUnit.SECONDS)

                    LogUtils.sendPurgeLog(context, messages)
                }
            }

    }
}