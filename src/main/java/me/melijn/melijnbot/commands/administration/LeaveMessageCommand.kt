package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
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
                MessageCommandUtil.showMessageContent(context, type)
            } else {
                MessageCommandUtil.setMessageContent(context, type)
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
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedTitle(context, type)
                    else -> MessageCommandUtil.setEmbedTitle(context, type)
                }
            }
        }

        class SetTitleUrlArg(root: String, val type: MessageType) : AbstractCommand("$root.settitleurl") {

            init {
                name = "setTitleUrl"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedTitleUrl(context, type)
                    else -> MessageCommandUtil.setEmbedTitleUrl(context, type)
                }
            }
        }


        class SetAuthorArg(root: String, val type: MessageType) : AbstractCommand("$root.setauthor") {

            init {
                name = "setAuthor"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedAuthor(context, type)
                    else -> MessageCommandUtil.setEmbedAuthor(context, type)
                }
            }
        }

        class SetAuthorIconArg(root: String, val type: MessageType) : AbstractCommand("$root.setauthoricon") {

            init {
                name = "setAuthorIcon"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedAuthorIcon(context, type)
                    else -> MessageCommandUtil.setEmbedAuthorIcon(context, type)
                }
            }
        }

        class SetAuthorUrlArg(root: String, val type: MessageType) : AbstractCommand("$root.setauthorurl") {

            init {
                name = "setAuthorUrl"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedAuthorUrl(context, type)
                    else -> MessageCommandUtil.setEmbedAuthorUrl(context, type)
                }
            }
        }


        class SetThumbnailArg(root: String, val type: MessageType) : AbstractCommand("$root.setthumbnail") {

            init {
                name = "setThumbnail"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedThumbnail(context, type)
                    else -> MessageCommandUtil.setEmbedThumbnail(context, type)
                }
            }
        }

        class SetImageArg(root: String, val type: MessageType) : AbstractCommand("$root.setimage") {

            init {
                name = "setImage"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedImage(context, type)
                    else -> MessageCommandUtil.setEmbedImage(context, type)
                }
            }
        }


        class FieldArg(root: String, val type: MessageType) : AbstractCommand("$root.field") {

            init {
                name = "field"
                children = arrayOf(
                    AddArg(root, type),
                    RemoveArg(root, type),
                    ListArg(root, type),
                    SetTitleArg(root, type),
                    SetValueArg(root, type),
                    SetInlineArg(root, type)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context, syntax)
            }

            class AddArg(root: String, val type: MessageType) : AbstractCommand("$root.add") {

                init {
                    name = "add"
                    aliases = arrayOf("addInline")
                }

                override suspend fun execute(context: CommandContext) {
                    val split = context.rawArg.split(">")
                    if (split.size < 2) {
                        sendSyntax(context, syntax)
                    }
                    val title = split[0]
                    val value = context.rawArg.replaceFirst("$title>", "")

                    val inline = context.commandParts[1].equals("addInline", true)
                    MessageCommandUtil.addEmbedField(title, value, inline, context, type)
                }
            }

            class SetTitleArg(root: String, val type: MessageType) : AbstractCommand("$root.settitle") {

                init {
                    name = "setTitle"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context, syntax)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    val title = context.rawArg.replaceFirst("$index\\s+?".toRegex(), "")
                    MessageCommandUtil.setEmbedFieldTitle(index, title, context, type)
                }
            }

            class SetValueArg(root: String, val type: MessageType) : AbstractCommand("$root.setvalue") {

                init {
                    name = "setValue"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context, syntax)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    val value = context.rawArg.replaceFirst("$index\\s+?".toRegex(), "")
                    MessageCommandUtil.setEmbedFieldValue(index, value, context, type)
                }
            }

            class SetInlineArg(root: String, val type: MessageType) : AbstractCommand("$root.setinline") {

                init {
                    name = "setInline"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context, syntax)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    val value = getBooleanFromArgNMessage(context, 1) ?: return
                    MessageCommandUtil.setEmbedFieldInline(index, value, context, type)
                }
            }

            class RemoveArg(root: String, val type: MessageType) : AbstractCommand("$root.remove") {

                init {
                    name = "remove"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context, syntax)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    MessageCommandUtil.removeEmbedField(index, context, type)
                }
            }

            class ListArg(root: String, val type: MessageType) : AbstractCommand("$root.list") {

                init {
                    name = "list"
                }

                override suspend fun execute(context: CommandContext) {
                    MessageCommandUtil.showEmbedFields(context, type)
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
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedDescription(context, type)
                    else -> MessageCommandUtil.setEmbedDescription(context, type)
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
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedColor(context, type)
                    else -> MessageCommandUtil.setEmbedColor(context, type)
                }
            }
        }

        class SetFooterArg(root: String, val type: MessageType) : AbstractCommand("$root.setfooter") {

            init {
                name = "setFooter"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedFooter(context, type)
                    else -> MessageCommandUtil.setEmbedFooter(context, type)
                }
            }
        }

        class SetFooterUrlArg(root: String, val type: MessageType) : AbstractCommand("$root.setfooterurl") {

            init {
                name = "setFooterUrl"
            }

            override suspend fun execute(context: CommandContext) {
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showEmbedFooterIcon(context, type)
                    else -> MessageCommandUtil.setEmbedFooterIcon(context, type)
                }
            }
        }

        class ClearArg(root: String, val type: MessageType) : AbstractCommand("$root.clear") {

            init {
                name = "clear"
                aliases = arrayOf("c")
            }

            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.clearEmbed(context, type)
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
                MessageCommandUtil.listAttachments(context, type)
            }
        }

        class AddArg(root: String, val type: MessageType) : AbstractCommand("$root.add") {

            init {
                name = "add"
            }

            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.addAttachment(context, type)
            }

        }

        class RemoveArg(root: String, val type: MessageType) : AbstractCommand("$root.remove") {

            init {
                name = "remove"
            }

            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.removeAttachment(context, type)
            }
        }

    }

}