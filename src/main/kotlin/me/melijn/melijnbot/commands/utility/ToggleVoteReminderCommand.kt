package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.database.settings.VoteReminderOption
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp

class ToggleVoteReminderCommand : AbstractCommand("command.togglevotereminder") {

    init {
        id = 199
        name = "toggleVoteReminder"
        children = arrayOf(
            ListArg(root)
        )
        aliases = arrayOf("tvr")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val wrapper = context.daoManager.voteReminderStatesWrapper
        val stateMap = wrapper.contains(context.authorId)
        val toggle = getEnumFromArgNMessage<VoteReminderOption>(context, 0, "$root.invalidoption") ?: return

        val cState = stateMap[toggle] ?: throw IllegalStateException("Yardim, report to bot dev pls")
        if (toggle.default == cState) {
            wrapper.enable(context.authorId, toggle)
        } else {
            wrapper.remove(context.authorId, toggle)
        }

        val msg = context.getTranslation("$root.set.${!cState}")
        sendRsp(context, msg)
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.voteReminderStatesWrapper
            val stateMap = wrapper.contains(context.authorId)
            var msg = ""
            for ((opt, state) in stateMap) {
                msg += "$opt: $state\n"
            }
            sendRsp(context, msg)
        }
    }
}