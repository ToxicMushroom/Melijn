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
        val denied = wrapper.contains(context.authorId)
        if (context.args.isEmpty() || context.args[0] != "view") {
            if (denied) {
                wrapper.remove(context.authorId)
            } else {
                wrapper.add(context.authorId)
            }

            val msg = context.getTranslation("$root.set.${!denied}")
            sendRsp(context, msg)
        } else {

            val msg = context.getTranslation("$root.show.$denied")
            sendRsp(context, msg)
        }
    }
}