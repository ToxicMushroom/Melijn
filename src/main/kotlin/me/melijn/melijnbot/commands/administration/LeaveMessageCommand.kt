package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.removeFirst
import net.dv8tion.jda.api.Permission

class LeaveMessageCommand : AbstractCommand("command.leavemessage") {

    init {
        id = 35
        name = "leaveMessage"
        aliases = arrayOf("lm")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(
            SetContentArg(root, MessageType.LEAVE),
            EmbedArg(root, MessageType.LEAVE),
            AttachmentsArg(root, MessageType.LEAVE),
            ViewArg(root, MessageType.LEAVE),
            SetPingableArg(root, MessageType.LEAVE)
        )
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class SetContentArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setcontent") {

        init {
            name = "setContent"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                MessageCommandUtil.showMessage(context, ModularMessageProperty.CONTENT, type)
            } else {
                MessageCommandUtil.setMessage(context, ModularMessageProperty.CONTENT, type)
            }
        }
    }

    class EmbedArg(parent: String, val type: MessageType) : AbstractCommand("$parent.embed") {

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
                SetFooterIconArg(root, type)
                //What even is optimization
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class SetTitleArg(parent: String, val type: MessageType) : AbstractCommand("$parent.settitle") {

            init {
                name = "setTitle"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_TITLE
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class SetTitleUrlArg(parent: String, val type: MessageType) : AbstractCommand("$parent.settitleurl") {

            init {
                name = "setTitleUrl"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_URL
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }


        class SetAuthorArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setauthor") {

            init {
                name = "setAuthor"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_AUTHOR
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class SetAuthorIconArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setauthoricon") {

            init {
                name = "setAuthorIcon"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_AUTHOR_ICON_URL
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class SetAuthorUrlArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setauthorurl") {

            init {
                name = "setAuthorUrl"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_AUTHOR_URL
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }


        class SetThumbnailArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setthumbnail") {

            init {
                name = "setThumbnail"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_THUMBNAIL
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class SetImageArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setimage") {

            init {
                name = "setImage"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_IMAGE
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }


        class FieldArg(parent: String, val type: MessageType) : AbstractCommand("$parent.field") {

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
                sendSyntax(context)
            }

            class AddArg(parent: String, val type: MessageType) : AbstractCommand("$parent.add") {

                init {
                    name = "add"
                    aliases = arrayOf("addInline")
                }

                override suspend fun execute(context: CommandContext) {
                    val split = context.rawArg.split(">")
                    if (split.size < 2) {
                        sendSyntax(context)
                    }
                    val title = split[0]
                    val value = context.rawArg.removeFirst("$title>")

                    val inline = context.commandParts[1].equals("addInline", true)
                    MessageCommandUtil.addEmbedField(title, value, inline, context, type)
                }
            }

            class SetTitleArg(parent: String, val type: MessageType) : AbstractCommand("$parent.settitle") {

                init {
                    name = "setTitle"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    val title = context.rawArg
                        .removeFirst("$index")
                        .trim()
                    MessageCommandUtil.setEmbedFieldTitle(index, title, context, type)
                }
            }

            class SetValueArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setvalue") {

                init {
                    name = "setValue"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    val value = context.rawArg
                        .removeFirst("$index")
                        .trim()
                    MessageCommandUtil.setEmbedFieldValue(index, value, context, type)
                }
            }

            class SetInlineArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setinline") {

                init {
                    name = "setInline"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    val value = getBooleanFromArgNMessage(context, 1) ?: return
                    MessageCommandUtil.setEmbedFieldInline(index, value, context, type)
                }
            }

            class RemoveArg(parent: String, val type: MessageType) : AbstractCommand("$parent.remove") {

                init {
                    name = "remove"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }
                    val index = getIntegerFromArgNMessage(context, 0) ?: return
                    MessageCommandUtil.removeEmbedField(index, context, type)
                }
            }

            class ListArg(parent: String, val type: MessageType) : AbstractCommand("$parent.list") {

                init {
                    name = "list"
                }

                override suspend fun execute(context: CommandContext) {
                    MessageCommandUtil.showEmbedFields(context, type)
                }
            }
        }


        class SetDescriptionArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setdescription") {

            init {
                name = "setDescription"
                aliases = arrayOf("setDesc")
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_DESCRIPTION
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class SetColorArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setcolor") {

            init {
                name = "setColor"
                aliases = arrayOf("setColour")
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_COLOR
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class SetFooterArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setfooter") {

            init {
                name = "setFooter"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_FOOTER
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class SetFooterIconArg(root: String, val type: MessageType) : AbstractCommand("$root.setfootericon") {

            init {
                name = "setFooterIcon"
            }

            override suspend fun execute(context: CommandContext) {
                val property = ModularMessageProperty.EMBED_FOOTER_ICON_URL
                when {
                    context.rawArg.isBlank() -> MessageCommandUtil.showMessage(context, property, type)
                    else -> MessageCommandUtil.setMessage(context, property, type)
                }
            }
        }

        class ClearArg(parent: String, val type: MessageType) : AbstractCommand("$parent.clear") {

            init {
                name = "clear"
                aliases = arrayOf("c")
            }

            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.clearEmbed(context, type)
            }
        }
    }

    class AttachmentsArg(parent: String, val type: MessageType) : AbstractCommand("$parent.attachments") {

        init {
            name = "attachments"
            aliases = arrayOf("a")
            children = arrayOf(
                ListArg(root, type),
                AddArg(root, type),
                RemoveArg(root, type)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class ListArg(parent: String, val type: MessageType) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                MessageCommandUtil.listAttachments(context, type)
            }
        }

        class AddArg(parent: String, val type: MessageType) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 2) {
                    sendSyntax(context, syntax)
                    return
                }
                MessageCommandUtil.addAttachment(context, type)
            }

        }

        class RemoveArg(parent: String, val type: MessageType) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("delete", "r", "d")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }
                MessageCommandUtil.removeAttachment(context, type)
            }
        }
    }

    class ViewArg(parent: String, val type: MessageType) : AbstractCommand("$parent.view") {

        init {
            name = "view"
            discordChannelPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)
            aliases = arrayOf("preview")
        }

        override suspend fun execute(context: CommandContext) {
            MessageCommandUtil.showMessagePreviewTyped(context, type)
        }
    }

    class SetPingableArg(parent: String, val type: MessageType) : AbstractCommand("$parent.setpingable") {

        init {
            name = "setPingable"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.rawArg.isBlank()) {
                MessageCommandUtil.showPingable(context, type)
                return
            }

            val pingable = getBooleanFromArgNMessage(context, 0) ?: return
            MessageCommandUtil.setPingable(context, type, pingable)
        }
    }
}