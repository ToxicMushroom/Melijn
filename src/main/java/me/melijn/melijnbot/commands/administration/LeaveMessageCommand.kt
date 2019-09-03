package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class LeaveMessageCommand  : AbstractCommand("command.leave") {

    init {
        id = 35
        name = "leaveMessage"
        aliases = arrayOf("lm")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(SetContentArg(root), EmbedArg(root), AttachmentsArg(root))
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class SetContentArg(root: String) : AbstractCommand("$root.setcontent") {

        init {
            name = "setContent"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                MessageCommandUtil.showMessageContent(this, context, MessageType.LEAVE)
            } else {
                MessageCommandUtil.setMessageContent(this, context, MessageType.LEAVE)
            }
        }
    }

    class EmbedArg(root: String) : AbstractCommand("$root.embed") {

        init {
            name = "embed"
            aliases = arrayOf("e")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class AttachmentsArg(root: String) : AbstractCommand("$root.attachments") {

        init {
            name = "attachments"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: CommandContext) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

}