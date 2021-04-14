package me.melijn.melijnbot.commands.moderation

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class PurgeCommand : AbstractCommand("command.purge") {

    private val silentPurgeName = "spurge"
    private val silentPruneName = "sprune"

    // set of guildId,channelId
    private val purgeInProgress = ConcurrentHashMap.newKeySet<Pair<Long, Long>>()

    init {
        id = 39
        name = "purge"
        aliases = arrayOf(silentPurgeName, silentPruneName, "prune")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)
        commandCategory = CommandCategory.MODERATION
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        // + 1 is to start counting above the .purge command
        val amount = (getIntegerFromArgNMessage(context, 0, 1, 1000) ?: return) + 1
        val targetUser = if (context.args.size == 2) {
            retrieveUserByArgsNMessage(context, 1) ?: return
        } else null

        val purgePID = Pair(context.guildId, context.channelId)
        if (purgeInProgress.contains(purgePID)) {
            val msg = context.getTranslation("$root.inprogress")
            sendRsp(context, msg)
            return
        }
        purgeInProgress.add(purgePID)

        val messages = mutableListOf<Message>()
        var counter = 1
        context.textChannel.iterableHistory.forEachAsync { message ->
            if ((targetUser == null || targetUser.idLong == message.author.idLong) && !message.isPinned) {
                messages.add(message)
            }
            if (amount <= counter) {
                false
            } else {
                counter++
                true
            }
        }.thenRun {
            TaskManager.async(context) {
                for (message in messages) {
                    context.container.purgedIds[message.idLong] = context.authorId
                }

                val msg = try {
                    val futures = context.textChannel.purgeMessages(messages)
                    futures.forEach {
                        it.await()
                    }

                    val userMore = if (targetUser == null) "" else ".user"
                    val more = if (amount > 1) ".more" else ".one"
                    context.getTranslation("$root.success$userMore$more")
                        .withVariable("amount", amount.toString())
                        .withSafeVariable(PLACEHOLDER_USER, targetUser?.asTag ?: "")
                } catch (t: Throwable) {
                    context.getTranslation("$root.error")
                }

                purgeInProgress.remove(purgePID)
                val invoke = context.commandParts[1]
                if (!invoke.isInside(
                        silentPruneName,
                        silentPurgeName,
                        ignoreCase = true
                    ) && context.textChannel.canTalk()
                ) {
                    sendMsgAwaitEL(context, msg)
                        .firstOrNull()
                        ?.delete()
                        ?.queueAfter(5, TimeUnit.SECONDS)
                }

                if (messages.isNotEmpty()) {
                    LogUtils.sendPurgeLog(context, messages)
                }
            }
        }
    }
}