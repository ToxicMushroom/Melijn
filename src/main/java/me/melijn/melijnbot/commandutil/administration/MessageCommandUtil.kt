package me.melijn.melijnbot.commandutil.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.MessageWrapper
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_TYPE
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getColorFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.toHex
import me.melijn.melijnbot.objects.utils.toLCC
import net.dv8tion.jda.api.EmbedBuilder
import org.json.JSONObject

object MessageCommandUtil {

    suspend fun removeMessageIfEmpty(guildId: Long, type: MessageType, message: ModularMessage, messageWrapper: MessageWrapper): Boolean {
        return if (messageWrapper.shouldRemove(message)) {
            messageWrapper.removeMessage(guildId, type)
            true
        } else {
            false
        }
    }

    suspend fun setMessageJoinLeave(context: CommandContext, property: ModularMessageProperty, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val guildId = context.getGuildId()
        val message = messageWrapper.messageCache.get(Pair(guildId, type)).await() ?: ModularMessage()

        runCorrectSetThing(property, context, message, type)
        messageWrapper.updateMessage(message, guildId, type)
    }

    suspend fun setMessageCC(context: CommandContext, property: ModularMessageProperty, cc: CustomCommand) {
        val messageWrapper = context.daoManager.messageWrapper
        val guildId = context.getGuildId()
        val message = cc.content
        val type = MessageType.CUSTOM_COMMAND

        runCorrectSetThing(property, context, message, type)
        cc.content = message

        messageWrapper.updateMessage(message, guildId, type)
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
        }
    }


    suspend fun showMessageJoinLeave(context: CommandContext, property: ModularMessageProperty, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val guildId = context.getGuildId()
        val message = messageWrapper.messageCache.get(Pair(guildId, type)).await()

        showMessage(context, property, message, type)
    }

    suspend fun showMessageCC(context: CommandContext, property: ModularMessageProperty, cc: CustomCommand) {
        showMessage(context, property, cc.content, MessageType.CUSTOM_COMMAND)
    }

    private suspend fun showMessage(context: CommandContext, property: ModularMessageProperty, message: ModularMessage?, type: MessageType) {
        val language = context.getLanguage()
        var path = ""
        val string: String? = when (property) {
            ModularMessageProperty.CONTENT -> {
                path = "message.content.show"
                message?.messageContent
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
                path = "message.embed.author.show"
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
        }

        val msg = if (string == null) {
            i18n.getTranslation(language, "$path.unset")
        } else {
            i18n.getTranslation(language, "$path.set")
                .replace("%${property.toLCC()}%", string)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }


    suspend fun clearEmbedJoinLeave(context: CommandContext, type: MessageType) {
        val language = context.getLanguage()
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()

        if (modularMessage != null) {
            messageWrapper.clearEmbed(modularMessage, context.getGuildId(), type)
        }

        val msg = i18n.getTranslation(language, "message.embed.clear")
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun listAttachmentsJoinLeave(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        listAttachmentsAndMessage(context, modularMessage, type)
    }

    suspend fun listAttachmentsAndMessage(context: CommandContext, message: ModularMessage?, type: MessageType) {
        val language = context.getLanguage()

        val msg = if (message == null || message.attachments.isEmpty()) {
            i18n.getTranslation(language, "message.attachments.list.empty")
                .replace(PLACEHOLDER_TYPE, type.text)

        } else {
            val title = i18n.getTranslation(language, "message.attachments.list.title")
                .replace(PLACEHOLDER_TYPE, type.text)
            var content = "\n```INI"
            for ((index, attachment) in message.attachments.entries.withIndex()) {
                content += "\n$index - [${attachment.key}] - ${attachment.value}"
            }
            content += "```"
            (title + content)
        }
        sendMsg(context, msg)
    }

    suspend fun addAttachmentJoinLeave(context: CommandContext, type: MessageType) {
        val language = context.getLanguage()
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val newMap = modularMessage.attachments.toMutableMap()
        newMap[context.args[0]] = context.args[1]

        modularMessage.attachments = newMap.toMap()

        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
        val msg = i18n.getTranslation(language, "message.attachments.add")
            .replace(PLACEHOLDER_TYPE, type.text)
            .replace("%attachment%", context.args[0])
            .replace("%file%", context.args[1])

        sendMsg(context, msg)
    }

    suspend fun removeAttachmentJoinLeave(context: CommandContext, type: MessageType) {
        val language = context.getLanguage()
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val attachments = modularMessage.attachments.toMutableMap()
        val file = if (attachments.containsKey(context.args[0])) attachments[context.args[0]] else null
        attachments.remove(context.args[0])

        modularMessage.attachments = attachments.toMap()

        val msg =
            if (file == null) {
                i18n.getTranslation(language, "message.attachments.remove.notanattachment")

            } else {
                messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
                i18n.getTranslation(language, "message.attachments.remove.success")
                    .replace("%file%", file)
            }.replace(PLACEHOLDER_ARG, context.args[0])
                .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setMessageContentAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            message.messageContent = null
            i18n.getTranslation(language, "message.content.unset")
        } else {
            message.messageContent = arg
            i18n.getTranslation(language, "message.content.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedDescriptionAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setDescription(null)
            i18n.getTranslation(language, "message.embed.description.unset")
        } else {
            eb.setDescription(arg)
            i18n.getTranslation(language, "message.embed.description.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedColorAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val color = getColorFromArgNMessage(context, 0) ?: return
        val language = context.getLanguage()
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setColor(null)
            i18n.getTranslation(language, "message.embed.color.unset")
        } else {
            eb.setColor(color)
            i18n.getTranslation(language, "message.embed.color.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedTitleAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setTitle(null)
            i18n.getTranslation(language, "message.embed.title.unset")
        } else {
            eb.setTitle(arg)
            i18n.getTranslation(language, "message.embed.title.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val title = message.embed?.title
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setTitle(title, null)
            i18n.getTranslation(language, "message.embed.titleurl.unset")
        } else {
            eb.setTitle(title, arg)
            i18n.getTranslation(language, "message.embed.titleurl.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedAuthorAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setAuthor(null)
            i18n.getTranslation(language, "message.embed.author.unset")
        } else {
            eb.setTitle(arg)
            i18n.getTranslation(language, "message.embed.author.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedAuthorIconUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val authorName = message.embed?.author?.name
        val authorUrl = message.embed?.author?.url
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setAuthor(authorName, authorUrl, null)
            i18n.getTranslation(language, "message.embed.authoriconurl.unset")
        } else {
            eb.setAuthor(authorName, authorUrl, arg)
            i18n.getTranslation(language, "message.embed.authoriconurl.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedAuthorUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val authorName = message.embed?.author?.name
        val iconUrl = message.embed?.author?.iconUrl
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setAuthor(authorName, null, iconUrl)
            i18n.getTranslation(language, "message.embed.authorurl.unset")
        } else {
            eb.setAuthor(authorName, arg, iconUrl)
            i18n.getTranslation(language, "message.embed.authorurl.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedThumbnailAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setThumbnail(null)
            i18n.getTranslation(language, "message.embed.thumbnail.unset")
        } else {
            eb.setThumbnail(arg)
            i18n.getTranslation(language, "message.embed.thumbnail.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedImageAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setImage(null)
            i18n.getTranslation(language, "message.embed.image.unset")
        } else {
            eb.setImage(arg)
            i18n.getTranslation(language, "message.embed.image.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }

    suspend fun setEmbedFooterAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val footerIconUrl = message.embed?.footer?.iconUrl
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setFooter(null, footerIconUrl)
            i18n.getTranslation(language, "message.embed.image.unset")
        } else {
            eb.setFooter(arg, footerIconUrl)
            i18n.getTranslation(language, "message.embed.image.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }


    suspend fun setEmbedFooterIconUrlAndMessage(context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val arg = context.rawArg
        val footer = message.embed?.footer?.text
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setFooter(footer, null)
            i18n.getTranslation(language, "message.embed.image.unset")
        } else {
            eb.setFooter(footer, arg)
            i18n.getTranslation(language, "message.embed.image.set")
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        message.embed = eb.build()
        sendMsg(context, msg)
    }


    suspend fun addEmbedFieldJoinLeave(title: String, value: String, inline: Boolean, context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        addEmbedFieldAndMessage(title, value, inline, context, modularMessage, type)
        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
    }

    suspend fun addEmbedFieldAndMessage(title: String, value: String, inline: Boolean, context: CommandContext, message: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val embedBuilder = EmbedBuilder(message.embed)
        embedBuilder.addField(title, value, inline)
        message.embed = embedBuilder.build()

        val inlineString = i18n.getTranslation(language, if (inline) "yes" else "no")
        val msg = i18n.getTranslation(language, "message.embed.field.add")
            .replace("%title%", title)
            .replace("%value%", value)
            .replace("%inline%", inlineString)
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedFieldTitleJoinLeave(index: Int, title: String, context: CommandContext, type: MessageType) {
        setEmbedFieldPart(index, "title", title, context, type)
    }

    suspend fun setEmbedFieldValueJoinLeave(index: Int, value: String, context: CommandContext, type: MessageType) {
        setEmbedFieldPart(index, "value", value, context, type)
    }

    suspend fun setEmbedFieldInlineJoinLeave(index: Int, inline: Boolean, context: CommandContext, type: MessageType) {
        setEmbedFieldPart(index, "inline", inline, context, type)
    }

    private suspend fun setEmbedFieldPart(index: Int, partName: String, value: Any, context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        setEmbedFieldPartAndMessage(index, partName, value, context, modularMessage, type)
        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
    }

    suspend fun setEmbedFieldPartAndMessage(index: Int, partName: String, value: Any, context: CommandContext, modularMessage: ModularMessage, type: MessageType) {
        val language = context.getLanguage()
        val json = JSONObject(modularMessage.toJSON())
        val embedJSON = json.getJSONObject("embed")
        val fieldsJSON = embedJSON.getJSONArray("fields")
        val field = fieldsJSON.getJSONObject(index)
        field.put(partName, value)
        fieldsJSON.put(index, field)
        embedJSON.put("fields", fieldsJSON)
        json.put("embed", embedJSON)
        val modularMessage1 = ModularMessage.fromJSON(json.toString(4))
        modularMessage.embed = modularMessage1.embed
        modularMessage.messageContent = modularMessage1.messageContent
        modularMessage.attachments = modularMessage1.attachments

        val partValue: String = when (value) {
            is Boolean -> i18n.getTranslation(language, if (value) "yes" else "no")
            else -> value.toString()
        }
        val msg = i18n.getTranslation(language, "message.embed.field$partName.set")
            .replace("%index%", index.toString())
            .replace("%$partName%", partValue)
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun removeEmbedFieldJoinLeave(index: Int, context: CommandContext, type: MessageType) {
        val language = context.getLanguage()
        val messageWrapper = context.daoManager.messageWrapper
        var modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val json = JSONObject(modularMessage.toJSON())
        val embedJSON = json.getJSONObject("embed")
        val fieldsJSON = embedJSON.getJSONArray("fields")
        fieldsJSON.remove(index)
        embedJSON.put("fields", fieldsJSON)
        json.put("embed", embedJSON)
        modularMessage = ModularMessage.fromJSON(json.toString(4))

        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
        val msg = i18n.getTranslation(language, "message.embed.field.removed")
            .replace("%index%", index.toString())
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedFieldsJoinLeave(context: CommandContext, type: MessageType) {
        val language = context.getLanguage()
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val fields = modularMessage?.embed?.fields

        val msg = if (fields == null || fields.isEmpty()) {
            i18n.getTranslation(language, "message.embed.field.list.empty")
        } else {
            val title = i18n.getTranslation(language, "message.embed.field.list.title")
            var desc = "```INI"
            for ((index, field) in fields.withIndex()) {
                desc += "\n$index - [${field.name}] - [${field.value}] - ${if (field.isInline) "true" else "\nfalse"}"
            }
            desc += "```"
            (title + desc)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }
}