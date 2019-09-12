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
            children = arrayOf(
                ClearArg(root, type),
                SetDescriptionArg(root, type),
                SetColorArg(root, type),
                SetTitleArg(root, type),
                SetTitleUrlArg(root, type),
                SetAuthorArg(root, type),
                SetAuthorIconArg(root, type),
                SetAuthorUrlArg(root, type),
                SetThumbnailArg(root, type),
                SetImageArg(root, type),
                FieldArg(root, type),
                SetFooterArg(root, type),
                SetFooterUrlArg(root, type)
            //What even is optimization
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class SetTitleArg(root: String, val type: MessageType) : AbstractCommand("$root.settitle") {

            init {
                name = "setTitle"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedTitle(this, context, type)
                    else -> MessageCommandUtil.setEmbedTitle(this, context, type)
                }
            }
        }

        class SetTitleUrlArg(root: String, val type: MessageType) : AbstractCommand("$root.settitleurl") {

            init {
                name = "setTitleUrl"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedTitleUrl(this, context, type)
                    else -> MessageCommandUtil.setEmbedTitleUrl(this, context, type)
                }
            }
        }


        class SetAuthorArg(root: String, val type: MessageType) : AbstractCommand("$root.setauthor") {

            init {
                name = "setAuthor"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedAuthor(this, context, type)
                    else -> MessageCommandUtil.setEmbedAuthor(this, context, type)
                }
            }
        }

        class SetAuthorIconArg(root: String, val type: MessageType) : AbstractCommand("$root.setauthoricon") {

            init {
                name = "setAuthorIcon"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedAuthorIcon(this, context, type)
                    else -> MessageCommandUtil.setEmbedAuthorIcon(this, context, type)
                }
            }
        }

        class SetAuthorUrlArg(root: String, val type: MessageType) : AbstractCommand("$root.setauthorurl") {

            init {
                name = "setAuthorUrl"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedAuthorUrl(this, context, type)
                    else -> MessageCommandUtil.setEmbedAuthorUrl(this, context, type)
                }
            }
        }


        class SetThumbnailArg(root: String, val type: MessageType) : AbstractCommand("$root.setthumbnail") {

            init {
                name = "setThumbnail"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedThumbnail(this, context, type)
                    else -> MessageCommandUtil.setEmbedThumbnail(this, context, type)
                }
            }
        }

        class SetImageArg(root: String, val type: MessageType) : AbstractCommand("$root.setimage") {

            init {
                name = "setImage"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedImage(this, context, type)
                    else -> MessageCommandUtil.setEmbedImage(this, context, type)
                }
            }
        }


        class FieldArg(root: String, val type: MessageType) : AbstractCommand("$root.field") {

            init {
                name = "field"
                children = arrayOf(
                    AddArg(root, type),
                    RemoveArg(root, type),
                    ListArg(root, type)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context, syntax)
            }

            class AddArg(root: String, val type: MessageType) : AbstractCommand("$root.add") {

                init {
                    name = "add"
                }

                override suspend fun execute(context: CommandContext) {

                }
            }

            class RemoveArg(root: String, val type: MessageType) : AbstractCommand("$root.remove") {

                init {
                    name = "remove"
                }

                override suspend fun execute(context: CommandContext) {

                }
            }

            class ListArg(root: String, val type: MessageType) : AbstractCommand("$root.list") {

                init {
                    name = "list"
                }

                override suspend fun execute(context: CommandContext) {

                }
            }
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

        class SetFooterArg(root: String, val type: MessageType) : AbstractCommand("$root.setfooter") {

            init {
                name = "setFooter"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedFooter(this, context, type)
                    else -> MessageCommandUtil.setEmbedFooter(this, context, type)
                }
            }
        }

        class SetFooterUrlArg(root: String, val type: MessageType) : AbstractCommand("$root.setfooterurl") {

            init {
                name = "setFooterUrl"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedFooterUrl(this, context, type)
                    else -> MessageCommandUtil.setEmbedFooterUrl(this, context, type)
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