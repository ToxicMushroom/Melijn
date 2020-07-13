package me.melijn.melijnbot.commandutil.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.MessageWrapper
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_TYPE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.utils.data.DataObject
import java.time.Instant

object MessageCommandUtil {

    suspend fun removeMessageIfEmpty(guildId: Long, type: MessageType, message: ModularMessage, messageWrapper: MessageWrapper): Boolean {
        return if (messageWrapper.shouldRemove(message)) {
            messageWrapper.removeMessage(guildId, type)
            true
        } else {
            false
        }
    }

    suspend fun setMessage(context: CommandContext, property: ModularMessageProperty, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val guildId = context.guildId
        val message = messageWrapper.messageCache.get(Pair(guildId, type)).await() ?: ModularMessage()

        runCorrectSetThing(property, context, message, type)
        messageWrapper.updateMessage(message, guildId, type)
    }

    suspend fun setMessageCC(context: CommandContext, property: ModularMessageProperty, cc: CustomCommand) {
        val ccWrapper = context.daoManager.customCommandWrapper
        val guildId = context.guildId
        val message = cc.content
        val type = MessageType.CUSTOM_COMMAND

        runCorrectSetThing(property, context, message, type)
        cc.content = message

        ccWrapper.update(guildId, cc)
    }

    private suspend fun runCorrectSetThing(property: ModularMessageProperty, context: CommandContext, message: ModularMessage, type: MessageType) {
        when (property) {
            ModularMessageProperty.CONTENT -> setMessageContentAndMessage(context, message, type)
            ModularMessageProperty.EMBED_DESCRIPTION -> setEmbedDescriptionAndMessage(context, message, type)
            ModularMessageProperty.EMBED_TITLE -> setEmbedTitleAndMessage(context, message, type)
            ModularMessageProperty.EMBED_URL -> setEmbedUrlAndMessage(context, message, type)
            ModularMessageProperty.EMBED_THUMBNAIL -> setEmbedThumbnailAndMessage(context, message, type)
            ModularMessageProperty.EMBED_IMAGE -> setEmbedImageAndMessage(context, message, type)
            ModularMessageProperty.EMBED_AUTHOR -> setEmbedAuthorAndMessage(context, message, type)
            ModularMessageProperty.EMBED_AUTHOR_URL -> setEmbedAuthorUrlAndMessage(context, message, type)
            ModularMessageProperty.EMBED_AUTHOR_ICON_URL -> setEmbedAuthorIconUrlAndMessage(context, message, type)
            ModularMessageProperty.EMBED_FOOTER -> setEmbedFooterAndMessage(context, message, type)
            ModularMessageProperty.EMBED_FOOTER_ICON_URL -> setEmbedFooterIconUrlAndMessage(context, message, type)
            ModularMessageProperty.EMBED_COLOR -> setEmbedColorAndMessage(context, message, type)
            ModularMessageProperty.EMBED_TIME_STAMP -> setEmbedTimeStampMessage(context, message, type)
        }
    }


    suspend fun showMessage(context: CommandContext, property: ModularMessageProperty, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val guildId = context.guildId
        val message = messageWrapper.messageCache.get(Pair(guildId, type)).await()

        showMessage(context, property, message, type)
    }

    suspend fun showMessageCC(context: CommandContext, property: ModularMessageProperty, cc: CustomCommand) {
        showMessage(context, property, cc.content, MessageType.CUSTOM_COMMAND)
    }

    private suspend fun showMessage(context: CommandContext, property: ModularMessageProperty, message: ModularMessage?, type: MessageType) {
        var path = ""
        var pathSuffix = ""
        val string: String? = when (property) {
            ModularMessageProperty.CONTENT -> {
                path = "message.content.show"
                message?.messageContent?.let { "```${it.replace("`", "´")}```" }
            }
            ModularMessageProperty.EMBED_DESCRIPTION -> {
                path = "message.embed.description.show"
                message?.embed?.description
            }
            ModularMessageProperty.EMBED_TITLE -> {
                path = "message.embed.title.show"
                message?.embed?.title
            }
            ModularMessageProperty.EMBED_URL -> {
                path = "message.embed.url.show"
                message?.embed?.url
            }
            ModularMessageProperty.EMBED_THUMBNAIL -> {
                path = "message.embed.thumbnail.show"
                message?.embed?.thumbnail?.url
            }
            ModularMessageProperty.EMBED_IMAGE -> {
                path = "message.embed.image.show"
                message?.embed?.image?.url
            }
            ModularMessageProperty.EMBED_AUTHOR -> {
                path = "message.embed.authorname.show"
                message?.embed?.author?.name
            }
            ModularMessageProperty.EMBED_AUTHOR_URL -> {
                path = "message.embed.authorurl.show"
                message?.embed?.author?.url
            }
            ModularMessageProperty.EMBED_AUTHOR_ICON_URL -> {
                path = "message.embed.authoriconurl.show"
                message?.embed?.author?.iconUrl
            }
            ModularMessageProperty.EMBED_FOOTER -> {
                path = "message.embed.footer.show"
                message?.embed?.footer?.text
            }
            ModularMessageProperty.EMBED_FOOTER_ICON_URL -> {
                path = "message.embed.footericonurl.show"
                message?.embed?.footer?.iconUrl
            }
            ModularMessageProperty.EMBED_COLOR -> {
                path = "message.embed.color.show"
                message?.embed?.color?.toHex()
            }
            ModularMessageProperty.EMBED_TIME_STAMP -> {
                path = "message.embed.timestamp.show"
                if (message?.extra?.containsKey("currentTimestamp") == true) pathSuffix = ".updating"
                message?.embed?.timestamp?.asLongLongGMTString()
            }
        }

        val msg = if (string == null) {
            context.getTranslation("$path.unset$pathSuffix")
        } else {
            context.getTranslation("$path.set$pathSuffix")
                .replace("%${property.variableName}%", string)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }


    suspend fun clearEmbed(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()
        clearEmbedAndMessage(context, type, modularMessage)
        messageWrapper.updateMessage(modularMessage, context.guildId, type)
    }

    suspend fun clearEmbedCC(context: CommandContext, cc: CustomCommand) {
        val ccWrapper = context.daoManager.customCommandWrapper
        val modularMessage = cc.content
        clearEmbedAndMessage(context, MessageType.CUSTOM_COMMAND, modularMessage)
        cc.content = modularMessage
        ccWrapper.update(context.guildId, cc)
    }

    private suspend fun clearEmbedAndMessage(context: CommandContext, type: MessageType, modularMessage: ModularMessage) {
        modularMessage.embed = null

        val msg = context.getTranslation("message.embed.clear")
            .withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }


    suspend fun listAttachments(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
        listAttachmentsAndMessage(context, modularMessage, type)
    }

    suspend fun listAttachmentsCC(context: CommandContext, cc: CustomCommand) {
        val modularMessage = cc.content
        listAttachmentsAndMessage(context, modularMessage, MessageType.CUSTOM_COMMAND)
    }

    private suspend fun listAttachmentsAndMessage(context: CommandContext, message: ModularMessage?, type: MessageType) {
        val msg = if (message == null || message.attachments.isEmpty()) {
            context.getTranslation("message.attachments.list.empty")
                .withVariable(PLACEHOLDER_TYPE, type.text)

        } else {
            val title = context.getTranslation("message.attachments.list.title")
                .withVariable(PLACEHOLDER_TYPE, type.text)
            var content = "\n```INI"
            for ((index, attachment) in message.attachments.entries.withIndex()) {
                content += "\n$index - [${attachment.key}] - ${attachment.value}"
            }
            content += "```"
            (title + content)
        }
        sendRsp(context, msg)
    }

    suspend fun addAttachment(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()

        addAttachmentAndMessage(context, type, modularMessage)
        messageWrapper.setMessage(context.guildId, type, modularMessage)
    }

    suspend fun addAttachmentCC(context: CommandContext, cc: CustomCommand) {
        val ccWrapper = context.daoManager.customCommandWrapper
        val modularMessage = cc.content
        addAttachmentAndMessage(context, MessageType.CUSTOM_COMMAND, modularMessage)
        cc.content = modularMessage
        ccWrapper.update(context.guildId, cc)
    }


    private suspend fun addAttachmentAndMessage(context: CommandContext, type: MessageType, modularMessage: ModularMessage) {
        val newMap = modularMessage.attachments.toMutableMap()
        newMap[context.args[0]] = context.args[1]

        modularMessage.attachments = newMap.toMap()

        val msg = context.getTranslation("message.attachments.add")
            .withVariable(PLACEHOLDER_TYPE, type.text)
            .withVariable("attachment", context.args[0])
            .withVariable("url", context.args[1])

        sendRsp(context, msg)
    }

    suspend fun removeAttachment(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()

        removeAttachmentAndMessage(context, type, modularMessage)
        messageWrapper.setMessage(context.guildId, type, modularMessage)
    }

    suspend fun removeAttachmentCC(context: CommandContext, cc: CustomCommand) {
        val ccWrapper = context.daoManager.customCommandWrapper
        val modularMessage = cc.content
        removeAttachmentAndMessage(context, MessageType.CUSTOM_COMMAND, modularMessage)
        cc.content = modularMessage
        ccWrapper.update(context.guildId, cc)
    }

    private suspend fun removeAttachmentAndMessage(context: CommandContext, type: MessageType, modularMessage: ModularMessage) {
        val attachments = modularMessage.attachments.toMutableMap()
        val file = if (attachments.containsKey(context.args[0])) {
            attachments[context.args[0]]
        } else {
            null
        }
        attachments.remove(context.args[0])

        modularMessage.attachments = attachments.toMap()

        val msg =
            if (file == null) {
                context.getTranslation("message.attachments.remove.notanattachment")
                    .withVariable("prefix", context.usedPrefix)
            } else {
                context.getTranslation("message.attachments.remove.success")
                    .withVariable("file", file)
            }.withVariable(PLACEHOLDER_ARG, context.args[0])
                .withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }

    private suspend fun setMessageContentAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            message.messageContent = null
            context.getTranslation("message.content.unset")
        } else {
            message.messageContent = arg
            context.getTranslation("message.content.set")
                .withVariable(PLACEHOLDER_ARG, "```${arg.replace("`", "´")}```")
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }

    private suspend fun setEmbedDescriptionAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setDescription(null)
            context.getTranslation("message.embed.description.unset")
        } else {
            eb.setDescription(arg)
            context.getTranslation("message.embed.description.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedColorAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val color = getColorFromArgNMessage(context, 0) ?: return
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setColor(null)
            context.getTranslation("message.embed.color.unset")
        } else {
            eb.setColor(color)
            context.getTranslation("message.embed.color.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedTimeStampMessage(context: CommandContext, message: ModularMessage, type: MessageType) {

        val timeArg = context.args[0]
        val eb = EmbedBuilder(message.embed)
        val mmap = message.extra.toMutableMap()

        val msg = if (timeArg.equals("null", true)) {
            eb.setTimestamp(null)
            mmap.remove("currentTimestamp")
            context.getTranslation("message.embed.timestamp.unset")
        } else {
            val millis = getDateTimeFromArgNMessage(context, 0) ?: return
            val state = getBooleanFromArgN(context, 1) ?: false
            val timeStamp = Instant.ofEpochMilli(millis)
            eb.setTimestamp(timeStamp)

            if (state) {
                mmap["currentTimestamp"] = ""
                context.getTranslation("message.embed.timestamp.set.updating")
                    .withVariable(PLACEHOLDER_ARG, timeArg)
            } else {
                mmap.remove("currentTimestamp")
                context.getTranslation("message.embed.timestamp.set")
                    .withVariable(PLACEHOLDER_ARG, timeArg)
            }
        }.withVariable(PLACEHOLDER_TYPE, type.text)


        message.extra = mmap
        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedTitleAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setTitle(null)
            context.getTranslation("message.embed.title.unset")
        } else {
            eb.setTitle(arg)
            context.getTranslation("message.embed.title.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val title = message.embed?.title
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setTitle(title, null)
            context.getTranslation("message.embed.titleurl.unset")
        } else {
            try {
                eb.setTitle(title, arg)
                context.getTranslation("message.embed.titleurl.set")
                    .withVariable(PLACEHOLDER_ARG, arg)
            } catch (t: Throwable) {
                context.getTranslation("message.embed.titleurl.invalid")
                    .withVariable(PLACEHOLDER_ARG, arg)
            }
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedAuthorAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setAuthor(null)
            context.getTranslation("message.embed.authorname.unset")
        } else {
            eb.setTitle(arg)
            context.getTranslation("message.embed.authorname.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedAuthorIconUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val authorName = message.embed?.author?.name
        val authorUrl = message.embed?.author?.url
        val eb = EmbedBuilder(message.embed)
        val attachments = context.message.attachments
        val user = retrieveUserByArgsN(context, 0)

        val msg = when {
            arg.equals("null", true) -> {
                eb.setAuthor(authorName, authorUrl, null)
                context.getTranslation("message.embed.authoriconurl.unset")
            }
            attachments.isNotEmpty() -> {
                eb.setAuthor(authorName, authorUrl, attachments[0].url)
                context.getTranslation("message.embed.authoriconurl.set")
                    .withVariable(PLACEHOLDER_ARG, attachments[0].url)
            }
            user != null -> {
                eb.setAuthor(authorName, authorUrl, user.effectiveAvatarUrl)
                context.getTranslation("message.embed.authoriconurl.set")
                    .withVariable(PLACEHOLDER_ARG, user.effectiveAvatarUrl)
            }
            else -> {
                eb.setAuthor(authorName, authorUrl, arg)
                context.getTranslation("message.embed.authoriconurl.set")
                    .withVariable(PLACEHOLDER_ARG, arg)
            }
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    suspend fun setEmbedAuthorUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val authorName = message.embed?.author?.name
        val iconUrl = message.embed?.author?.iconUrl
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setAuthor(authorName, null, iconUrl)
            context.getTranslation("message.embed.authorurl.unset")
        } else {
            eb.setAuthor(authorName, arg, iconUrl)
            context.getTranslation("message.embed.authorurl.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedThumbnailAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)
        val attachments = context.message.attachments
        val user = retrieveUserByArgsN(context, 0)

        val msg = when {
            arg.equals("null", true) -> {
                eb.setThumbnail(null)
                context.getTranslation("message.embed.thumbnail.unset")
            }
            attachments.isNotEmpty() -> {
                eb.setThumbnail(attachments[0].url)
                context.getTranslation("message.embed.thumbnail.set")
                    .withVariable(PLACEHOLDER_ARG, attachments[0].url)
            }
            user != null -> {
                eb.setThumbnail(user.effectiveAvatarUrl)
                context.getTranslation("message.embed.thumbnail.set")
                    .withVariable(PLACEHOLDER_ARG, user.effectiveAvatarUrl)
            }
            else -> {
                eb.setThumbnail(arg)
                context.getTranslation("message.embed.thumbnail.set")
                    .withVariable(PLACEHOLDER_ARG, arg)
            }
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedImageAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)
        val attachments = context.message.attachments
        val user = retrieveUserByArgsN(context, 0)

        val msg = when {
            arg.equals("null", true) -> {
                eb.setImage(null)
                context.getTranslation("message.embed.image.unset")
            }
            attachments.isNotEmpty() -> {
                eb.setImage(attachments[0].url)
                context.getTranslation("message.embed.image.set")
                    .withVariable(PLACEHOLDER_ARG, attachments[0].url)
            }
            user != null -> {
                eb.setImage(user.effectiveAvatarUrl)
                context.getTranslation("message.embed.image.set")
                    .withVariable(PLACEHOLDER_ARG, user.effectiveAvatarUrl)
            }
            else -> {
                try {
                    eb.setImage(arg)
                    context.getTranslation("message.embed.image.set")
                        .withVariable(PLACEHOLDER_ARG, arg)
                } catch (t: IllegalArgumentException) {
                    context.getTranslation("message.embed.image.urlerror")
                        .withVariable(PLACEHOLDER_ARG, arg)
                }
            }
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedFooterAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val footerIconUrl = message.embed?.footer?.iconUrl
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setFooter(null, footerIconUrl)
            context.getTranslation("message.embed.image.unset")
        } else {
            eb.setFooter(arg, footerIconUrl)
            context.getTranslation("message.embed.image.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }


    private suspend fun setEmbedFooterIconUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val arg = context.rawArg
        val footer = message.embed?.footer?.text
        val eb = EmbedBuilder(message.embed)
        val attachments = context.message.attachments
        val user = retrieveUserByArgsN(context, 0)

        val msg = when {
            arg.equals("null", true) -> {
                eb.setFooter(footer, null)
                context.getTranslation("message.embed.footericon.unset")
            }
            attachments.isNotEmpty() -> {
                eb.setFooter(footer, attachments[0].url)
                context.getTranslation("message.embed.footericon.set")
                    .withVariable(PLACEHOLDER_ARG, attachments[0].url)
            }
            user != null -> {
                eb.setFooter(footer, user.effectiveAvatarUrl)
                context.getTranslation("message.embed.footericon.set")
                    .withVariable(PLACEHOLDER_ARG, user.effectiveAvatarUrl)
            }
            else -> {
                eb.setFooter(footer, arg)
                context.getTranslation("message.embed.footericon.set")
                    .withVariable(PLACEHOLDER_ARG, arg)
            }
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendRsp(context, msg)
    }


    suspend fun addEmbedField(title: String, value: String, inline: Boolean, context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()

        addEmbedFieldAndMessage(title, value, inline, context, modularMessage, type)
        messageWrapper.setMessage(context.guildId, type, modularMessage)
    }

    suspend fun addEmbedFieldCC(title: String, value: String, inline: Boolean, context: CommandContext, customCommand: CustomCommand) {
        val ccWrapper = context.daoManager.customCommandWrapper
        val modularMessage = customCommand.content

        addEmbedFieldAndMessage(title, value, inline, context, modularMessage, MessageType.CUSTOM_COMMAND)
        ccWrapper.update(context.guildId, customCommand)
    }

    private suspend fun addEmbedFieldAndMessage(title: String, value: String, inline: Boolean, context: CommandContext, message: ModularMessage, type: MessageType) {
        val embedBuilder = EmbedBuilder(message.embed)
        embedBuilder.addField(title, value, inline)
        message.embed = embedBuilder.build()

        val inlineString = context.getTranslation(if (inline) "yes" else "no")
        val msg = context.getTranslation("message.embed.field.add")
            .withVariable("title", title)
            .withVariable("value", value)
            .withVariable("inline", inlineString)
            .withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }

    suspend fun setEmbedFieldTitleCC(index: Int, title: String, context: CommandContext, cc: CustomCommand) {
        setEmbedFieldPartCC(index, "title", title, context, cc)
    }

    suspend fun setEmbedFieldValueCC(index: Int, value: String, context: CommandContext, cc: CustomCommand) {
        setEmbedFieldPartCC(index, "value", value, context, cc)
    }

    suspend fun setEmbedFieldInlineCC(index: Int, inline: Boolean, context: CommandContext, cc: CustomCommand) {
        setEmbedFieldPartCC(index, "inline", inline, context, cc)
    }

    private suspend fun setEmbedFieldPartCC(index: Int, partName: String, value: Any, context: CommandContext, cc: CustomCommand) {
        val messageWrapper = context.daoManager.customCommandWrapper
        val modularMessage = cc.content

        setEmbedFieldPartAndMessage(index, partName, value, context, modularMessage, MessageType.CUSTOM_COMMAND)
        cc.content = modularMessage

        messageWrapper.update(context.guildId, cc)
    }

    suspend fun setEmbedFieldTitle(index: Int, title: String, context: CommandContext, type: MessageType) {
        setEmbedFieldPartJoinLeave(index, "title", title, context, type)
    }

    suspend fun setEmbedFieldValue(index: Int, value: String, context: CommandContext, type: MessageType) {
        setEmbedFieldPartJoinLeave(index, "value", value, context, type)
    }

    suspend fun setEmbedFieldInline(index: Int, inline: Boolean, context: CommandContext, type: MessageType) {
        setEmbedFieldPartJoinLeave(index, "inline", inline, context, type)
    }

    private suspend fun setEmbedFieldPartJoinLeave(index: Int, partName: String, value: Any, context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()

        setEmbedFieldPartAndMessage(index, partName, value, context, modularMessage, type)
        messageWrapper.setMessage(context.guildId, type, modularMessage)
    }

    private suspend fun setEmbedFieldPartAndMessage(index: Int, partName: String, value: Any, context: CommandContext, modularMessage: ModularMessage, type: MessageType) {
        val json = DataObject.fromJson(modularMessage.toJSON())
        val embedJSON = json.getObject("embed")
        val fieldsJSON = embedJSON.getArray("fields")
        val field = fieldsJSON.getObject(index)
        field.put(partName, value)
        fieldsJSON.insert(index, field)
        embedJSON.put("fields", fieldsJSON)
        json.put("embed", embedJSON)
        val modularMessage1 = ModularMessage.fromJSON(json.toString())
        modularMessage.embed = modularMessage1.embed
        modularMessage.messageContent = modularMessage1.messageContent
        modularMessage.attachments = modularMessage1.attachments

        val partValue: String = when (value) {
            is Boolean -> context.getTranslation(if (value) "yes" else "no")
            else -> value.toString()
        }
        val msg = context.getTranslation("message.embed.field$partName.set")
            .withVariable("index", index.toString())
            .replace("%$partName%", partValue)
            .withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }

    suspend fun removeEmbedField(index: Int, context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()

        removeEmbedFieldAndMessage(index, context, type, modularMessage)

        messageWrapper.setMessage(context.guildId, type, modularMessage)
    }

    suspend fun removeEmbedFieldCC(index: Int, context: CommandContext, cc: CustomCommand) {
        val ccWrapper = context.daoManager.customCommandWrapper

        val message = cc.content
        removeEmbedFieldAndMessage(index, context, MessageType.CUSTOM_COMMAND, message)
        cc.content = message

        ccWrapper.update(context.guildId, cc)
    }

    private suspend fun removeEmbedFieldAndMessage(index: Int, context: CommandContext, type: MessageType, modularMessage: ModularMessage) {
        var modularMessage1 = modularMessage

        val json = DataObject.fromJson(modularMessage1.toJSON())
        val embedJSON = json.getObject("embed")
        val fieldsJSON = embedJSON.getArray("fields")
        fieldsJSON.remove(index)
        embedJSON.put("fields", fieldsJSON)
        json.put("embed", embedJSON)
        modularMessage1 = ModularMessage.fromJSON(json.toString())
        modularMessage.embed = modularMessage1.embed
        modularMessage.messageContent = modularMessage1.messageContent
        modularMessage.attachments = modularMessage1.attachments

        val msg = context.getTranslation("message.embed.field.removed")
            .withVariable("index", index.toString())
            .withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }


    suspend fun showEmbedFields(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()

        showEmbedFieldsAndMessage(context, type, modularMessage)
    }

    suspend fun showEmbedFieldsCC(context: CommandContext, cc: CustomCommand) {
        val message = cc.content
        showEmbedFieldsAndMessage(context, MessageType.CUSTOM_COMMAND, message)
    }


    private suspend fun showEmbedFieldsAndMessage(context: CommandContext, type: MessageType, modularMessage: ModularMessage) {
        val fields = modularMessage.embed?.fields

        val msg = if (fields == null || fields.isEmpty()) {
            context.getTranslation("message.embed.field.list.empty")
        } else {
            val title = context.getTranslation("message.embed.field.list.title")
            var desc = "```INI"
            for ((index, field) in fields.withIndex()) {
                desc += "\n$index - [${field.name}] - [${field.value}] - ${if (field.isInline) "true" else "\nfalse"}"
            }
            desc += "```"
            (title + desc)
        }.withVariable(PLACEHOLDER_TYPE, type.text)

        sendRsp(context, msg)
    }

    suspend fun showMessagePreviewTyped(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val guildId = context.guildId
        val message = messageWrapper.messageCache.get(Pair(guildId, type)).await()?.toMessage()
        if (message == null) {
            val msg2 = context.getTranslation("message.view.isempty")
                .withVariable("type", type.toUCC())
            sendRsp(context, msg2)
            return
        }

        context.textChannel.sendMessage(message).queue()
    }


    suspend fun showMessagePreviewCC(context: CommandContext, cc: CustomCommand) {
        val msg = cc.content.toMessage()
        if (msg == null) {
            val msg2 = context.getTranslation("message.view.cc.isempty")
                .withVariable("ccId", cc.id.toString())
                .withVariable("ccName", cc.name)

            context.textChannel.sendMessage(msg2).queue()
        } else {
            context.textChannel.sendMessage(msg).queue()
        }
    }

    suspend fun setPingable(context: CommandContext, type: MessageType, pingable: Boolean) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.guildId, type)).await()
            ?: ModularMessage()

        setPingableAndMessage(context, modularMessage, type, pingable)
        messageWrapper.setMessage(context.guildId, type, modularMessage)
    }

    suspend fun setPingableCC(context: CommandContext, customCommand: CustomCommand, pingable: Boolean) {
        val ccWrapper = context.daoManager.customCommandWrapper
        val modularMessage = customCommand.content

        setPingableAndMessage(context, modularMessage, MessageType.CUSTOM_COMMAND, pingable)
        ccWrapper.update(context.guildId, customCommand)
    }

    private suspend fun setPingableAndMessage(context: CommandContext, message: ModularMessage, type: MessageType, pingable: Boolean) {
        val muteableMap = message.extra.toMutableMap()
        if (pingable) {
            muteableMap["isPingable"] = ""
        } else {
            muteableMap.remove("isPingable")
        }

        message.extra = muteableMap

        val msg = context.getTranslation("message.pingable.set.$pingable")
            .withVariable("type", type.text)
        sendRsp(context, msg)
    }

    suspend fun showPingable(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val guildId = context.guildId
        val message = messageWrapper.messageCache.get(Pair(guildId, type)).await()
            ?: ModularMessage()
        val isPingable = message.extra.containsKey("isPingable")

        val msg = context.getTranslation("message.pingable.show.$isPingable")
            .withVariable("type", type.text)
        sendRsp(context, msg)
    }

    suspend fun showPingableCC(context: CommandContext, cc: CustomCommand) {
        val messageWrapper = context.daoManager.customCommandWrapper
        val message = cc.content
        val isPingable = message.extra.containsKey("isPingable")
        cc.content = message

        messageWrapper.update(context.guildId, cc)

        val msg = context.getTranslation("message.pingable.show.$isPingable")
        sendRsp(context, msg)
    }
}