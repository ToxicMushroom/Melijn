package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.database.settings.VoteReminderOption
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable

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

    suspend fun execute(context: ICommandContext) {
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
            .withVariable("option", mapVoteReminderOptionToNiceText(toggle))
        sendRsp(context, msg)
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
        }

        suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.voteReminderStatesWrapper
            val stateMap = wrapper.contains(context.authorId)
            val eb = Embedder(context)
                .setTitle("VoteReminder List")
            for ((opt, state) in stateMap) {
                val stateMsg = if (state) "**enabled**" else "disabled"
                eb.appendDescription("`${mapVoteReminderOptionToNiceText(opt)}`: $stateMsg\n")
            }
            sendEmbedRsp(context, eb.build())
        }
    }
}

fun mapVoteReminderOptionToNiceText(voteReminderOption: VoteReminderOption): String {
    return when (voteReminderOption) {
        VoteReminderOption.TOPGG -> "TopGG"
        VoteReminderOption.DBLCOM -> "DblCom"
        VoteReminderOption.BFDCOM -> "BfdCom"
        VoteReminderOption.DBOATS -> "DBoats"
        VoteReminderOption.GLOBAL -> "Global"
    }
}
