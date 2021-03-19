package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageUtil
import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

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
            TODO("Not yet implemented")
        }
    }

    inner class EditArg(parent: String) : AbstractCommand("$parent.edit") {

        init {
            name = "edit"
            children = arrayOf(
                ContentArg(root),
                EmbedArg(root),
                AttachmentArg(root)
            )
            aliases = arrayOf("e")
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
                TODO("Not yet implemented")
            }
        }

        inner class AttachmentArg(parent: String) : AbstractCommand("$parent.attachment") {

            override suspend fun execute(context: ICommandContext) {
                TODO("Not yet implemented")
            }
        }

        inner class EmbedArg(parent: String) : AbstractCommand("$parent.embed") {

            init {
                name = "embed"
                aliases = arrayOf("e")
                children = arrayOf(
                    SetTitleArg(parent),
                    SetTitleUrlArg(parent),
                    SetAuthorArg(parent),
                    SetAuthorIconArg(parent),
                    SetAuthorUrlArg(parent),
                    SetThumbnailArg(parent),
                    SetImageArg(parent),
                    FieldArg(parent),
                    SetDescriptionArg(parent),
                    SetColorArg(parent),
                    SetFooterArg(parent),
                    SetFooterIconArg(parent),
                    ClearArg(parent)
                )
            }

            override suspend fun execute(context: ICommandContext) {
                sendSyntax(context)
            }

            inner class SetTitleArg(parent: String) : AbstractCommand("$parent.settitle") {

                init {
                    name = "setTitle"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_TITLE
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class SetTitleUrlArg(parent: String) : AbstractCommand("$parent.settitleurl") {

                init {
                    name = "setTitleUrl"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_URL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }


            inner class SetAuthorArg(parent: String) : AbstractCommand("$parent.setauthor") {

                init {
                    name = "setAuthor"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class SetAuthorIconArg(parent: String) :
                AbstractCommand("$parent.setauthoricon") {

                init {
                    name = "setAuthorIcon"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_ICON_URL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class SetAuthorUrlArg(parent: String) :
                AbstractCommand("$parent.setauthorurl") {

                init {
                    name = "setAuthorUrl"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_URL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }


            inner class SetThumbnailArg(parent: String) :
                AbstractCommand("$parent.setthumbnail") {

                init {
                    name = "setThumbnail"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_THUMBNAIL
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class SetImageArg(parent: String) : AbstractCommand("$parent.setimage") {

                init {
                    name = "setImage"
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
                    children = arrayOf(
                        AddArg(root),
                        RemoveArg(root),
                        ListArg(root),
                        SetTitleArg(root),
                        SetValueArg(root),
                        SetInlineArg(root)
                    )
                }

                override suspend fun execute(context: ICommandContext) {
                    sendSyntax(context)
                }

                inner class AddArg(parent: String) : AbstractCommand("$parent.add") {

                    init {
                        name = "add"
                        aliases = arrayOf("addInline")
                    }

                    override suspend fun execute(context: ICommandContext) {
                        val split = context.rawArg.split(">")
                        if (split.size < 2) {
                            sendSyntax(context)
                        }
                        val title = split[0].trim()
                        val value = context.rawArg.removeFirst("$title>").trim()

                        val inline = context.commandParts.getOrNull(4)
                            ?.equals("addInline", true) ?: false
                        MessageUtil.addEmbedField(title, value, inline, context)
                    }
                }

                inner class SetTitleArg(parent: String) : AbstractCommand("$parent.settitle") {

                    init {
                        name = "setTitle"
                    }

                    override suspend fun execute(context: ICommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val title = context.rawArg
                            .removeFirst("$index")
                            .trim()
                        MessageUtil.setEmbedFieldTitle(index, title, context)
                    }
                }

                inner class SetValueArg(parent: String) : AbstractCommand("$parent.setvalue") {

                    init {
                        name = "setValue"
                    }

                    override suspend fun execute(context: ICommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val value = context.rawArg
                            .removeFirst("$index")
                            .trim()
                        MessageUtil.setEmbedFieldValue(index, value, context)
                    }
                }

                inner class SetInlineArg(parent: String) : AbstractCommand("$parent.setinline") {

                    init {
                        name = "setInline"
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

                inner class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                    init {
                        name = "remove"
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
                    }

                    override suspend fun execute(context: ICommandContext) {
                        MessageUtil.showEmbedFields(context)
                    }
                }
            }


            inner class SetDescriptionArg(parent: String) :
                AbstractCommand("$parent.setdescription") {

                init {
                    name = "setDescription"
                    aliases = arrayOf("setDesc")
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_DESCRIPTION
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class SetColorArg(parent: String) : AbstractCommand("$parent.setcolor") {

                init {
                    name = "setColor"
                    aliases = arrayOf("setColour")
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_COLOR
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class SetFooterArg(parent: String) : AbstractCommand("$parent.setfooter") {

                init {
                    name = "setFooter"
                }

                override suspend fun execute(context: ICommandContext) {
                    val property = ModularMessageProperty.EMBED_FOOTER
                    when {
                        context.rawArg.isBlank() -> MessageUtil.showMessage(context, property)
                        else -> MessageUtil.setMessage(context, property)
                    }
                }
            }

            inner class SetFooterIconArg(root: String) : AbstractCommand("$root.setfootericon") {

                init {
                    name = "setFooterIcon"
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
                    aliases = arrayOf("c")
                }

                override suspend fun execute(context: ICommandContext) {
                    MessageUtil.clearEmbed(context)
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
                context.daoManager.driverManager.setCacheEntry("selectedMessage:$guildId", msgName, NORMAL_CACHE)
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