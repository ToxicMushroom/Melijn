package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class ToggleVoteReminderCommand : AbstractCommand("command.togglevotereminder") {

    init {
        id = 199
        name = "toggleVoteReminder"
        aliases = arrayOf("tvr")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.denyVoteReminderWrapper
        val current = wrapper.contains(context.authorId)
        if (context.args.isEmpty() || context.args[0] != "view") {
            if (current) {
                wrapper.remove(context.authorId)
            } else {
                wrapper.add(context.authorId)
            }

            val msg = context.getTranslation("$root.set.${!current}")
            sendRsp(context, msg)
        } else {

            val msg = context.getTranslation("$root.show.$current")
            sendRsp(context, msg)
        }
    }
}