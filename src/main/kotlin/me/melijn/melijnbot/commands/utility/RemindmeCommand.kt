package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.database.reminder.Reminder
import me.melijn.melijnbot.internals.arguments.ArgumentMode
import me.melijn.melijnbot.internals.arguments.annotations.CommandArg
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresGuildPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock

class RemindmeCommand : AbstractCommand("command.remindme") {

    companion object {
        private const val REMINDER_LIMIT = 5
        private const val PREMIUM_REMINDER_LIMIT = 40
        private const val REMINDER_LIMIT_PATH = "premium.feature.reminders.limit"
    }

    init {
        id = 226
        name = "remindme"
        aliases = arrayOf("remind", "reminder")
        children = arrayOf(
            ListArg(root),
            RemoveAtArg(root)
        )
        cooldown = 2000
        commandCategory = CommandCategory.UTILITY
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma")
        }

        suspend fun execute(context: ICommandContext) {
            val reminderWrapper = context.daoManager.reminderWrapper
            val reminders = reminderWrapper.getRemindersOfUser(context.authorId)
            if (reminders.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendRsp(context, msg)
                return
            }

            val index = (getIntegerFromArgNMessage(context, 0, 1, reminders.size) ?: return) - 1
            val reminderToRemove = reminders.sortedBy { it.remindAt }[index]

            context.daoManager.reminderWrapper.remove(context.authorId, reminderToRemove.remindAt)

            val msg = context.getTranslation("$root.removed")
                .withSafeVariable("index", index + 1)
                .withSafeVariable("message", reminderToRemove.message)
            sendRsp(context, msg)
        }

    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
        }

        suspend fun execute(context: ICommandContext) {
            val reminderWrapper = context.daoManager.reminderWrapper
            val reminders = reminderWrapper.getRemindersOfUser(context.authorId)
            if (reminders.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendRsp(context, msg)
                return
            }

            var list = "Your reminders:\n```INI"
            for ((index, reminder) in reminders.sortedBy { it.remindAt }.withIndex()) {
                val (_, remindAt, message) = reminder
                list += "\n${index + 1} -" +
                    " [${getDurationString(remindAt - System.currentTimeMillis())}] -" +
                    " [${remindAt.asEpochMillisToDateTime(context.getTimeZoneId())}]:" +
                    " ${message.escapeMarkdown().escapeDiscordInvites().take(256)}"
            }
            list += "```"

            sendRspCodeBlock(context, list, "INI", true)
        }
    }

    suspend fun execute(
        context: ICommandContext,
        @CommandArg(index = 0) duration: Long,
        @CommandArg(index = 1, mode = ArgumentMode.GREEDY) reason: String
    ) {
        if (duration < 10) {
            sendRsp(context, "Please use brain to remind you of things under 10 seconds.")
            return
        }

        val durationMillis = duration * 1000
        context.initCooldown()

        val reminderWrapper = context.daoManager.reminderWrapper
        val reminders = reminderWrapper.getRemindersOfUser(context.authorId)

        if (reminders.size > REMINDER_LIMIT && !isPremiumUser(context)) {
            val replaceMap = mapOf(
                "limit" to "$REMINDER_LIMIT",
                "premiumLimit" to "$PREMIUM_REMINDER_LIMIT"
            )

            sendFeatureRequiresGuildPremiumMessage(context, REMINDER_LIMIT_PATH, replaceMap)
            return
        } else if (reminders.size >= PREMIUM_REMINDER_LIMIT) {
            val msg = context.getTranslation("$root.limit.total")
                .withVariable("limit", "$PREMIUM_REMINDER_LIMIT")
            sendRsp(context, msg)
            return
        }

        reminderWrapper.add(Reminder(context.authorId, System.currentTimeMillis() + durationMillis, reason))

        val msg = "Reminder added, will remind you at **%time%** about `%thing%`"
            .withSafeVariable(
                "time",
                (System.currentTimeMillis() + durationMillis).asEpochMillisToDateTime(context.getTimeZoneId())
            )
            .withSafeVariable("thing", reason)
        sendRsp(context, msg)
    }
}