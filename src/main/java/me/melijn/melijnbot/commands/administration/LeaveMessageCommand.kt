package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendSyntax

class LeaveMessageCommand : AbstractCommand("command.leave") {

    init {
        id = 35
        name = "leaveMessage"
        aliases = arrayOf("lm")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(
                SetContentArg(root, MessageType.LEAVE),
                EmbedArg(root, MessageType.LEAVE),
                AttachmentsArg(root, MessageType.LEAVE)
        )
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class SetContentArg(root: String, val type: MessageType) : AbstractCommand("$root.setcontent") {

        init {
            name = "setContent"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                MessageCommandUtil.showMessageContent(this, context, type)
            } else {
                MessageCommandUtil.setMessageContent(this, context, type)
            }
        }
    }

    class EmbedArg(root: String, val type: MessageType) : AbstractCommand("$root.embed") {

        init {
            name = "embed"
            aliases = arrayOf("e")
            children = arrayOf(ClearArg(root, type), SetDescriptionArg(root, type), SetColorArg(root, type))
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class SetDescriptionArg(root: String, val type: MessageType) : AbstractCommand("$root.setdescription") {

            init {
                name = "setDescription"
                aliases = arrayOf("setDesc")
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedDescription(this, context, type)
                    else -> MessageCommandUtil.setEmbedDescription(this, context, type)
                }
            }
        }

        class SetColorArg(root: String, val type: MessageType) : AbstractCommand("$root.setcolor") {

            init {
                name = "setColor"
                aliases = arrayOf("setColour")
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedColor(this, context, type)
                    else -> MessageCommandUtil.setEmbedColor(this, context, type)
                }
            }
        }

        class ClearArg(root: String, val type: MessageType) : AbstractCommand("$root.clear") {

            init {
                name = "clear"
                aliases = arrayOf("c")
            }

            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.clearEmbed(this, context, type)
            }
        }
    }

    class AttachmentsArg(root: String, val type: MessageType) : AbstractCommand("$root.attachments") {

        init {
            name = "attachments"
            aliases = arrayOf("a")
            children = arrayOf(ListArg(root, type), AddArg(root, type), RemoveArg(root, type))
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class ListArg(root: String, val type: MessageType) : AbstractCommand("$root.list") {

            init {
                name = "list"
            }
            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.listAttachments(this, context, type)
            }
        }

        class AddArg(root: String, val type: MessageType) : AbstractCommand("$root.add") {

            init {
                name = "add"
            }
            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.addAttachment(this, context, type)
            }

        }

        class RemoveArg(root: String, val type: MessageType) : AbstractCommand("$root.remove") {

            init {
                name = "remove"
            }
            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.removeAttachment(this, context, type)
            }
        }

    }

}