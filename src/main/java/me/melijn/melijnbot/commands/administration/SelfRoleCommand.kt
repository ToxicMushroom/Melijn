package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getEmoteByArgsNMessage
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage

class SelfRoleCommand : AbstractCommand("command.selfrole") {

    init {
        id = 37
        name = "selfRole"
        aliases = arrayOf("sr")
        children = arrayOf(
            AddArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {

    }

    class AddArg(root: String) : AbstractCommand("$root.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            val role = getRoleByArgsNMessage(context, 0) ?: return
            val emote = getEmoteByArgsNMessage(context, 1) ?: return

            context.
        }
    }
}