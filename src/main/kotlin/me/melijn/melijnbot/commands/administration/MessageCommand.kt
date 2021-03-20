package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageUtil
import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.MessageEmbed

class MessageCommand : AbstractCommand("command.message") {

    init {
        name = "message"
        aliases = arrayOf("msg")
        children = arrayOf(
            ListArg(root),
            AddArg(root),
            SelectArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            EditArg(root),
            ViewArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class ViewArg(parent: String) : AbstractCommand("$parent.view") {

        init {
            name = "view"
            aliases = arrayOf("vw", "v")
        }

        override suspend fun execute(context: ICommandContext) {
            val selected = MessageUtil.getSelectedMessage(context) ?: return
            MessageUtil.showMessagePreviewTyped(context, selected)
        }
    }

    inner class EditArg(parent: String) : AbstractCommand("$parent.edit") {

        init {
            name = "edit"
            children = arrayOf(
                ContentArg(root),
                EmbedArg(root),
                AttachmentsArg(root),
                PingableArg(root)
            )
            aliases = arrayOf("e")
        }

        inner class PingableArg(parent: String) : AbstractCommand("$parent.pingable") {

            init {
                name = "pingable"
                aliases = arrayOf("pings")
            }

            override suspend fun execute(context: ICommandContext) {
                val msgName = MessageUtil.getSelectedMessage(context) ?: return
                if (context.rawArg.isBlank()) {
                    MessageUtil.showPingable(context, msgName)
                    return
                }

                val pingable = getBooleanFromArgNMessage(context, 0) ?: return
                MessageUtil.setPingable(context, msgName, pingable)
            }
        }


        override suspend fun execute(context: ICommandContext) {
            sendSyntax(context)
        }

        inner class ContentArg(parent: String) : AbstractCommand("$parent.content") {

            init {
                name = "content"
                aliases = arrayOf("setContent", "sc", "c")
            }

            override suspend fun execute(context: ICommandContext) {
                if (context.args.isEmpty()) {
                    MessageUtil.showMessage(context, ModularMessageProperty.CONTENT)
                } else {
                    MessageUtil.setMessage(context, ModularMessageProperty.CONTENT)
                }
            }
        }

        inner class AttachmentsArg(parent: String) : AbstractCommand("$parent.attachments") {

            init {
                name = "attachments"
                aliases = arrayOf("attach")
                children = arrayOf(
                    AddArg(root),
                    RemoveArg(root),
                    RemoveAtArg(root),
                    ListArg(root)
                )
            }

            override suspend fun execute(context: ICommandContext) {
                sendSyntax(context)
            }

            inner class AddArg(parent: String) : AbstractCommand("$parent.add") {

                init {
                    name = "add"
                    aliases = arrayOf("a")
                }

                override suspend fun execute(context: ICommandContext) {
                    MessageUtil.addAttachment(context)
                }
            }

            inner class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                init {
                    name = "remove"
                    aliases = arrayOf("rm")
                }

                override suspend fun execute(context: ICommandContext) {
                    MessageUtil.removeAttachment(context)
                }
            }

            inner class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeAt") {

                init {
                    name = "removeAt"
                    aliases = arrayOf("rma")
                }

                override suspend fun execute(context: ICommandContext) {
                    MessageUtil.removeAttachmentAt(context)
                }
            }

            inner class ListArg(parent: String) : AbstractCommand("$parent.list") {

                init {
                    name = "list"
                    aliases = arrayOf("ls")
                }

                override suspend fun execute(context: ICommandContext) {
                    MessageUtil.listAttachments(context)
                }
            }
        }

        inner class EmbedArg(parent: String) : AbstractCommand("$parent.embed") {

            init {
                name = "embed"
                aliases = arrayOf("e")
                children = arrayOf(
                    TitleArg(root),
                    TitleUrlArg(root),
                    AuthorArg(root),
                    AuthorIconArg(root),
                    AuthorUrlArg(root),
                    ThumbnailArg(root),
                    ImageArg(root),
                    FieldArg(root),
                    DescriptionArg(root),
                    ColorArg(root),
                    FooterArg(root),
                    FooterIconArg(root),
                    ClearArg(root),
                    TimeStampArg(root)
                )
            }

            override suspend fun execute(context: ICommandContext) {
                sendSyntax(context)
            }

            inner class TitleArg(parent: String) : AbstractCommand("$parent.title") {

                init {
                    name = "title"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_TITLE
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class TitleUrlArg(parent: String) : AbstractCommand("$parent.titleurl") {

                init {
                    name = "titleUrl"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_URL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }


            inner class AuthorArg(parent: String) : AbstractCommand("$parent.author") {

                init {
                    name = "author"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class AuthorIconArg(parent: String) : AbstractCommand("$parent.authoricon") {

                init {
                    name = "authorIcon"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_ICON_URL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class AuthorUrlArg(parent: String) : AbstractCommand("$parent.authorurl") {

                init {
                    name = "authorUrl"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_URL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }


            inner class ThumbnailArg(parent: String) : AbstractCommand("$parent.thumbnail") {

                init {
                    name = "thumbnail"
                    aliases = arrayOf("thumb")
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_THUMBNAIL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class ImageArg(parent: String) : AbstractCommand("$parent.image") {

                init {
                    name = "image"
                    aliases = arrayOf("img")
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_IMAGE
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }


            inner class FieldArg(parent: String) : AbstractCommand("$parent.field") {

                init {
                    name = "field"
                    aliases = arrayOf("f")
                    children = arrayOf(
                        AddArg(root),
                        RemoveAtArg(root),
                        ListArg(root),
                        TitleArg(root),
                        ValueArg(root),
                        InlineArg(root)
                    )
                }

                override suspend fun execute(context: ICommandContext) {
                    sendSyntax(context)
                }

                inner class AddArg(parent: String) : AbstractCommand("$parent.add") {

                    init {
                        name = "add"
                        aliases = arrayOf("addInline", "a")
                    }

                    override suspend fun execute(context: ICommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val title = getStringFromArgsNMessage(context, 0, 1, MessageEmbed.TITLE_MAX_LENGTH) ?: return
                        val value = getStringFromArgsNMessage(context, 1, 1, MessageEmbed.VALUE_MAX_LENGTH) ?: return

                        val inline = if (context.args.size == 2) {
                            context.commandParts.getOrNull(4)
                                ?.equals("addInline", true) ?: false
                        } else {
                            getBooleanFromArgNMessage(context, 2) ?: return
                        }
                        MessageUtil.addEmbedField(title, value, inline, context)
                    }
                }

                inner class TitleArg(parent: String) : AbstractCommand("$parent.title") {

                    init {
                        name = "title"
                        aliases = arrayOf("t")
                    }

                    override suspend fun execute(context: ICommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val title = context.fullArg
                            .removeFirst("$index")
                            .trim()
                        MessageUtil.setEmbedFieldTitle(index, title, context)
                    }
                }

                inner class ValueArg(parent: String) : AbstractCommand("$parent.value") {

                    init {
                        name = "value"
                        aliases = arrayOf("v")
                    }

                    override suspend fun execute(context: ICommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val value = context.fullArg
                            .removeFirst("$index")
                            .trim()
                        MessageUtil.setEmbedFieldValue(index, value, context)
                    }
                }

                inner class InlineArg(parent: String) : AbstractCommand("$parent.inline") {

                    init {
                        name = "inline"
                    }

                    override suspend fun execute(context: ICommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val value = getBooleanFromArgNMessage(context, 1) ?: return
                        MessageUtil.setEmbedFieldInline(index, value, context)
                    }
                }

                inner class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

                    init {
                        name = "removeAt"
                        aliases = arrayOf("rma", "rm", "remove")
                    }

                    override suspend fun execute(context: ICommandContext) {
                        if (context.args.isEmpty()) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        MessageUtil.removeEmbedField(index, context)
                    }
                }

                inner class ListArg(parent: String) : AbstractCommand("$parent.list") {

                    init {
                        name = "list"
                        aliases = arrayOf("ls")
                    }

                    override suspend fun execute(context: ICommandContext) {
                        MessageUtil.showEmbedFields(context)
                    }
                }
            }


            inner class DescriptionArg(parent: String) :
                AbstractCommand("$parent.description") {

                init {
                    name = "description"
                    aliases = arrayOf("desc")
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_DESCRIPTION
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class ColorArg(parent: String) : AbstractCommand("$parent.color") {

                init {
                    name = "color"
                    aliases = arrayOf("colour")
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_COLOR
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class FooterArg(parent: String) : AbstractCommand("$parent.footer") {

                init {
                    name = "footer"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_FOOTER
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class FooterIconArg(root: String) : AbstractCommand("$root.footericon") {

                init {
                    name = "footerIcon"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_FOOTER_ICON_URL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

                init {
                    name = "clear"
                }

                override suspend fun execute(context: ICommandContext) {
                    MessageUtil.clearEmbed(context)
                }
            }


            inner class TimeStampArg(parent: String) : AbstractCommand("$parent.timestamp") {

                init {
                    name = "timeStamp"
                    aliases = arrayOf("time")
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_TIME_STAMP
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma")
        }

        override suspend fun execute(context: ICommandContext) {
            val messages = context.daoManager.messageWrapper.getMessages(context.guildId)
            val index = getIntegerFromArgNMessage(context, 0, 1, messages.size) ?: return
            val msgName = messages[index - 1]
            context.daoManager.messageWrapper.removeMessage(context.guildId, msgName)
            val msg = context.getTranslation("$root.removed")
                .withSafeVariable("msgName", msgName)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm")
        }

        override suspend fun execute(context: ICommandContext) {
            val msgName = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
            val messages = context.daoManager.messageWrapper.getMessages(context.guildId)
            val match = messages.firstOrNull {
                it.equals(msgName, true)
            }
            if (match != null) {
                context.daoManager.messageWrapper.removeMessage(context.guildId, match)
                val msg = context.getTranslation("$root.removed")
                    .withSafeVariable("msgName", msgName)
                sendRsp(context, msg)
            } else {
                val msg = context.getTranslation("${context.commandOrder.first().root}.msgnoexist")
                    .withSafeVariable("msg", msgName)
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
            }
        }
    }

    class SelectArg(parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
            aliases = arrayOf("s", "sel")
        }

        override suspend fun execute(context: ICommandContext) {
            val msgName = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
            val messages = context.daoManager.messageWrapper.getMessages(context.guildId)
            if (msgName.isInside(messages, true)) {
                val guildId = context.guildId
                context.daoManager.driverManager.setCacheEntry("selectedMessage:$guildId", msgName, HIGHER_CACHE)
                val msg = context.getTranslation("$root.selected")
                    .withSafeVariable("msgName", msgName)
                sendRsp(context, msg)
            } else {
                val msg = context.getTranslation("${context.commandOrder.first().root}.msgnoexist")
                    .withSafeVariable("msg", msgName)
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
            }
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: ICommandContext) {
            val msgName = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
            val messages = context.daoManager.messageWrapper.getMessages(context.guildId)
            if (msgName.isInside(messages, true)) {
                val msg = context.getTranslation("$root.alreadyexists")
                    .withSafeVariable("msg", msgName)
                sendRsp(context, msg)
            } else {
                context.daoManager.messageWrapper.setMessage(context.guildId, msgName, ModularMessage())
                val msg = context.getTranslation("$root.added")
                    .withSafeVariable("msg", msgName)
                    .withSafeVarInCodeblock("msgInSyntax", msgName)
                    .withSafeVarInCodeblock(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
            }
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val messages = context.daoManager.messageWrapper.getMessages(context.guildId)
            if (messages.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                    .withSafeVarInCodeblock(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
                return
            }

            val msg = context.getTranslation("$root.list")
            val msgList = messages.withIndex().joinToString("\n") { "${it.index + 1} - [${it.value}]" }
            sendRsp(context, msg.withSafeVarInCodeblock("list", msgList))
        }
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}