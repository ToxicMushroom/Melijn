package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.getLongFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage

class ManageSupportersCommand : AbstractCommand("command.managesupporters") {

    init {
        id = 189
        name = "manageSupporters"
        aliases = arrayOf("ms")
        children = arrayOf(
            AddArg(root),
            SetArg(root),
            RemoveArg(root),
            ListArg(root)
        )
        commandCategory = CommandCategory.DEVELOPER
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            val user = retrieveUserByArgsNMessage(context, 0) ?: return

            context.daoManager.supporterWrapper.add(user.idLong)
            sendMsg(context, "Added ${user.asTag} as supporter.")
        }
    }

    class SetArg(parent: String) : AbstractCommand("$parent.set") {

        init {
            name = "set"
            aliases = arrayOf("s", "put", "p")
        }

        override suspend fun execute(context: CommandContext) {
            val user = retrieveUserByArgsNMessage(context, 0) ?: return
            val guildId = getLongFromArgNMessage(context, 1, 0) ?: return

            context.daoManager.supporterWrapper.setGuild(user.idLong, guildId)

            sendMsg(context, "Added/Updated ${user.asTag} as supporter with $guildId as linked guild.")
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("deleted", "del", "d", "rm", "rem", "r")
        }

        override suspend fun execute(context: CommandContext) {
            val user = retrieveUserByArgsNMessage(context, 0) ?: return

            context.daoManager.supporterWrapper.remove(user.idLong)
            sendMsg(context, "Removed ${user.asTag} from supporters.")
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "l")
        }

        override suspend fun execute(context: CommandContext) {
            val supporters = context.daoManager.supporterWrapper.getUsers()

            var msg = "```INI\n[userId] - [guildId] - [lastServerPicked] - [startTime]\n"
            for (supporterId in supporters) {
                val supporter = context.daoManager.supporterWrapper.getSupporter(supporterId) ?: continue
                msg += "${supporter.userId} (${context.shardManager.retrieveUserById(supporter.userId).awaitOrNull()?.asTag?.replace("#", "//") ?: "unknown"}) -" +
                    " ${if (supporter.guildId == -1L) "/" else supporter.guildId.toString()} -" +
                    " [${supporter.lastServerPickTime.asEpochMillisToDateTime(context.daoManager, context.guildId, context.authorId)}] -" +
                    " [${supporter.startMillis.asEpochMillisToDateTime(context.daoManager, context.guildId, context.authorId)}]\n"
            }
            msg += "```"

            sendRspCodeBlock(context, msg, "INI", false)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}