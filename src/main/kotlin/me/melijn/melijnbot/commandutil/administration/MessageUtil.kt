package me.melijn.melijnbot.commandutil.administration

import me.melijn.melijnbot.database.NORMAL_CACHE
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.jagtag.DiscordMethods
import me.melijn.melijnbot.internals.jagtag.UrlJagTagParser
import me.melijn.melijnbot.internals.jagtag.UrlParserArgs
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.models.UserFriendlyException
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_TYPE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendAttachments
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgWithAttachments
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import java.time.Instant

object MessageUtil {

    suspend fun setMessage(context: ICommandContext, property: ModularMessageProperty) {
        val guildId = context.guildId
        val selectedMessage = getSelectedMessage(context) ?: return

        val messageWrapper = context.daoManager.messageWrapper
        val message = messageWrapper.getMessage(guildId, selectedMessage) ?: ModularMessage()

        runCorrectSetThing(property, context, message, selectedMessage)
        messageWrapper.updateMessage(message, guildId, selectedMessage)
    }

    suspend fun showMessage(context: ICommandContext, property: ModularMessageProperty) {
        val guildId = context.guildId
        val selectedMessage = getSelectedMessage(context) ?: return

        val messageWrapper = context.daoManager.messageWrapper
        val message = messageWrapper.getMessage(guildId, selectedMessage)

        showMessage(context, property, message, selectedMessage)
    }

    suspend fun getSelectedMessage(context: ICommandContext): String? {
        val guildId = context.guildId
        val msgName = context.daoManager.driverManager.getCacheEntry("selectedMessage:$guildId", NORMAL_CACHE)
        if (msgName == null) {
            sendRsp(
                context, "You dont have any message selected, use `%prefix%msg select <msgName*>`"
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            )
            return null
        }

        return msgName
    }

    private suspend fun runCorrectSetThing(
        property: ModularMessageProperty,
        context: ICommandContext,
        message: ModularMessage,
        msgName: String
    ) {
        when (property) {
            ModularMessageProperty.CONTENT -> setMessageContentAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_DESCRIPTION -> setEmbedDescriptionAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_TITLE -> setEmbedTitleAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_URL -> setEmbedUrlAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_THUMBNAIL -> setEmbedThumbnailAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_IMAGE -> setEmbedImageAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_AUTHOR -> setEmbedAuthorAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_AUTHOR_URL -> setEmbedAuthorUrlAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_AUTHOR_ICON_URL -> setEmbedAuthorIconUrlAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_FOOTER -> setEmbedFooterAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_FOOTER_ICON_URL -> setEmbedFooterIconUrlAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_COLOR -> setEmbedColorAndMessage(context, message, msgName)
            ModularMessageProperty.EMBED_TIME_STAMP -> setEmbedTimeStampMessage(context, message, msgName)
        }
    }

    private suspend fun showMessage(
        context: ICommandContext,
        property: ModularMessageProperty,
        message: ModularMessage?,
        msgName: String
    ) {
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
                path = "message.embed.titleurl.show"
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
                path = "message.embed.footericon.show"
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    suspend fun clearEmbed(context: ICommandContext) {
        val messageWrapper = context.daoManager.messageWrapper
        val selectedMessage = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, selectedMessage)
            ?: ModularMessage()
        clearEmbedAndMessage(context, selectedMessage, modularMessage)
        messageWrapper.updateMessage(modularMessage, context.guildId, selectedMessage)
    }

    private suspend fun clearEmbedAndMessage(
        context: ICommandContext,
        msgName: String,
        modularMessage: ModularMessage
    ) {
        modularMessage.embed = null

        val msg = context.getTranslation("message.embed.clear")
            .withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    suspend fun listAttachments(context: ICommandContext) {
        val messageWrapper = context.daoManager.messageWrapper
        val msgName = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, msgName)
        listAttachmentsAndMessage(context, modularMessage, msgName)
    }

    private suspend fun listAttachmentsAndMessage(
        context: ICommandContext,
        message: ModularMessage?,
        msgName: String
    ) {
        val msg = if (message == null || message.attachments.isEmpty()) {
            context.getTranslation("message.attachments.list.empty")
                .withVariable(PLACEHOLDER_TYPE, msgName)

        } else {
            val title = context.getTranslation("message.attachments.list.title")
                .withVariable(PLACEHOLDER_TYPE, msgName)
            var content = "\n```INI"
            for ((index, attachment) in message.attachments.entries.withIndex().sortedBy { it.index }) {
                content += "\n${index + 1} - [${attachment.key}] - ${attachment.value}"
            }
            content += "```"
            (title + content)
        }
        sendRsp(context, msg)
    }

    suspend fun addAttachment(context: ICommandContext) {
        val messageWrapper = context.daoManager.messageWrapper
        val msgName = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, msgName)
            ?: ModularMessage()

        addAttachmentAndMessage(context, msgName, modularMessage)
        messageWrapper.setMessage(context.guildId, msgName, modularMessage)
    }

    private suspend fun addAttachmentAndMessage(
        context: ICommandContext,
        msgName: String,
        modularMessage: ModularMessage
    ) {
        if (argSizeCheckFailed(context, 1)) return
        val newMap = modularMessage.attachments.toMutableMap()
        val url = context.args[0]
        val isVariable = DiscordMethods.imgUrlMethods.any { "{${it.name}}".equals(url, true) }
        val msg = if (isVariable || EmbedBuilder.URL_PATTERN.matcher(url).matches()) {
            val fileName = context.args[1]
            newMap[url] = fileName

            modularMessage.attachments = newMap.toMap()
            context.getTranslation("message.attachments.add")
                .withVariable(PLACEHOLDER_TYPE, msgName)
                .withVariable("name", fileName)
                .withVariable("url", url)
        } else {
            context.getTranslation("message.embed.image.urlerror")
                .withVariable(PLACEHOLDER_ARG, url)
        }

        sendRsp(context, msg)
    }

    suspend fun removeAttachment(context: ICommandContext) {
        val messageWrapper = context.daoManager.messageWrapper
        val msgName = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, msgName)
            ?: ModularMessage()

        removeAttachmentAndMessage(context, msgName, modularMessage)
        messageWrapper.setMessage(context.guildId, msgName, modularMessage)
    }

    private suspend fun removeAttachmentAndMessage(
        context: ICommandContext,
        msgName: String,
        modularMessage: ModularMessage
    ) {
        if (argSizeCheckFailed(context, 1)) return
        val attachments = modularMessage.attachments.toMutableMap()
        val file = if (attachments.containsKey(context.args[0])) {
            attachments[context.args[0]]
        } else {
            null
        }
        attachments.remove(context.args[0])

        modularMessage.attachments = attachments.toMap()

        val msg = if (file == null) {
            context.getTranslation("message.attachments.remove.notanattachment")
                .withVariable("prefix", context.usedPrefix)
        } else {
            context.getTranslation("message.attachments.remove.success")
                .withVariable("file", file)
        }.withVariable(PLACEHOLDER_ARG, context.args[0])
            .withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    suspend fun removeAttachmentAt(context: ICommandContext) {
        val messageWrapper = context.daoManager.messageWrapper
        val msgName = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, msgName)
            ?: ModularMessage()

        removeAttachmentAtAndMessage(context, msgName, modularMessage)
        messageWrapper.setMessage(context.guildId, msgName, modularMessage)
    }

    private suspend fun removeAttachmentAtAndMessage(
        context: ICommandContext,
        msgName: String,
        modularMessage: ModularMessage
    ) {
        val entries = modularMessage.attachments.entries.withIndex().sortedBy { it.index }
        val index = getIntegerFromArgNMessage(context, 0, 1, entries.size) ?: return
        val (url, name) = entries.first { it.index == (index-1) }.value
        val attachments = modularMessage.attachments.toMutableMap()
        attachments.remove(url)
        modularMessage.attachments = attachments.toMap()

        val msg = context.getTranslation("message.attachments.remove.success")
            .withVariable("file", name)
            .withVariable(PLACEHOLDER_ARG, url)
            .withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    private suspend fun setMessageContentAndMessage(
        context: ICommandContext,
        message: ModularMessage,
        msgName: String
    ) {
        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            message.messageContent = null
            context.getTranslation("message.content.unset")
        } else {
            message.messageContent = arg
            context.getTranslation("message.content.set")
                .withVariable(PLACEHOLDER_ARG, "```${arg.replace("`", "´")}```")
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    private suspend fun setEmbedDescriptionAndMessage(
        context: ICommandContext,
        message: ModularMessage,
        msgName: String
    ) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setDescription(null)
            context.getTranslation("message.embed.description.unset")
        } else {
            eb.setDescription(arg)
            context.getTranslation("message.embed.description.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedColorAndMessage(context: ICommandContext, message: ModularMessage, msgName: String) {
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedTimeStampMessage(context: ICommandContext, message: ModularMessage, msgName: String) {

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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.extra = mmap
        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedTitleAndMessage(context: ICommandContext, message: ModularMessage, msgName: String) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        if (arg.length > MessageEmbed.TITLE_MAX_LENGTH) {
            val msg = context.getTranslation("message.embed.title.toolong")
                .withVariable("arg", arg)
                .withVariable("length", arg.length)
                .withVariable("max", MessageEmbed.TITLE_MAX_LENGTH)
            sendRsp(context, msg)
            return
        }

        val msg = if (arg.equals("null", true)) {
            eb.setTitle(null)
            context.getTranslation("message.embed.title.unset")
        } else {
            eb.setTitle(arg)
            context.getTranslation("message.embed.title.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = try {
            eb.build()
        } catch (e: IllegalStateException) {
            null
        }
        sendRsp(context, msg)
    }

    private suspend fun setEmbedUrlAndMessage(context: ICommandContext, message: ModularMessage, msgName: String) {
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedAuthorAndMessage(context: ICommandContext, message: ModularMessage, msgName: String) {
        val arg = context.rawArg
        val eb = EmbedBuilder(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setAuthor(null)
            context.getTranslation("message.embed.authorname.unset")
        } else {
            eb.setAuthor(arg)
            context.getTranslation("message.embed.authorname.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedAuthorIconUrlAndMessage(
        context: ICommandContext,
        message: ModularMessage,
        msgName: String
    ) {
        val arg = context.rawArg
        val authorName = message.embed?.author?.name
        val authorUrl = message.embed?.author?.url
        val eb = EmbedEditor(message.embed)
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    suspend fun setEmbedAuthorUrlAndMessage(context: ICommandContext, message: ModularMessage, msgName: String) {
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedThumbnailAndMessage(
        context: ICommandContext,
        message: ModularMessage,
        msgName: String
    ) {
        val arg = context.rawArg
        val eb = EmbedEditor(message.embed)
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedImageAndMessage(context: ICommandContext, message: ModularMessage, msgName: String) {
        val arg = context.rawArg
        val eb = EmbedEditor(message.embed)
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        try { // Fixes the cannot build empty embed error
            message.embed = eb.build()
        } catch (t: IllegalStateException) {
            t.printStackTrace()
            message.embed = null
        }
        sendRsp(context, msg)
    }

    private suspend fun setEmbedFooterAndMessage(context: ICommandContext, message: ModularMessage, msgName: String) {
        val arg = context.rawArg
        val footerIconUrl = message.embed?.footer?.iconUrl
        val eb = EmbedEditor(message.embed)

        val msg = if (arg.equals("null", true)) {
            eb.setFooter(null, footerIconUrl)
            context.getTranslation("message.embed.footer.unset")
        } else {
            eb.setFooter(arg, footerIconUrl)
            context.getTranslation("message.embed.footer.set")
                .withVariable(PLACEHOLDER_ARG, arg)
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    private suspend fun setEmbedFooterIconUrlAndMessage(
        context: ICommandContext,
        message: ModularMessage,
        msgName: String
    ) {
        val arg = context.rawArg
        val footer = message.embed?.footer?.text
        val eb = EmbedEditor(message.embed)
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        message.embed = eb.build()
        sendRsp(context, msg)
    }

    suspend fun addEmbedField(
        title: String,
        value: String,
        inline: Boolean,
        context: ICommandContext
    ) {
        val messageWrapper = context.daoManager.messageWrapper
        val selectedMessage = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, selectedMessage)
            ?: ModularMessage()

        addEmbedFieldAndMessage(title, value, inline, context, modularMessage)
        messageWrapper.setMessage(context.guildId, selectedMessage, modularMessage)
    }

    private suspend fun addEmbedFieldAndMessage(
        title: String,
        value: String,
        inline: Boolean,
        context: ICommandContext,
        message: ModularMessage,
    ) {
        val selectedMessage = getSelectedMessage(context) ?: return
        val embedBuilder = EmbedBuilder(message.embed)
        embedBuilder.addField(title, value, inline)
        message.embed = embedBuilder.build()

        val inlineString = context.getTranslation(if (inline) "yes" else "no")
        val msg = context.getTranslation("message.embed.field.add")
            .withVariable("title", title)
            .withVariable("value", value)
            .withVariable("inline", inlineString)
            .withVariable(PLACEHOLDER_TYPE, selectedMessage)

        sendRsp(context, msg)
    }

    suspend fun setEmbedFieldTitle(index: Int, title: String, context: ICommandContext) {
        val selectedMessage = getSelectedMessage(context) ?: return
        setEmbedFieldPartJoinLeave(index, "title", title, context, selectedMessage)
    }

    suspend fun setEmbedFieldValue(index: Int, value: String, context: ICommandContext) {
        val selectedMessage = getSelectedMessage(context) ?: return
        setEmbedFieldPartJoinLeave(index, "value", value, context, selectedMessage)
    }

    suspend fun setEmbedFieldInline(index: Int, inline: Boolean, context: ICommandContext) {
        val selectedMessage = getSelectedMessage(context) ?: return
        setEmbedFieldPartJoinLeave(index, "inline", inline, context, selectedMessage)
    }

    private suspend fun setEmbedFieldPartJoinLeave(
        index: Int,
        partName: String,
        value: Any,
        context: ICommandContext,
        msgName: String
    ) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.getMessage(context.guildId, msgName)
            ?: ModularMessage()

        setEmbedFieldPartAndMessage(index, partName, value, context, modularMessage, msgName)
        messageWrapper.setMessage(context.guildId, msgName, modularMessage)
    }

    private suspend fun setEmbedFieldPartAndMessage(
        index: Int,
        partName: String,
        value: Any,
        context: ICommandContext,
        modularMessage: ModularMessage,
        msgName: String
    ) {
        val json = DataObject.fromJson(modularMessage.toJSON())
        val embedJSON = json.getObject("embed")
        val fieldsJSON = embedJSON.getArray("fields")
        val field = fieldsJSON.getObject(index)
        field.put(partName, value)
        fieldsJSON.remove(index)
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
            .withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    suspend fun removeEmbedField(index: Int, context: ICommandContext) {
        val messageWrapper = context.daoManager.messageWrapper
        val selectedMessage = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, selectedMessage)
            ?: ModularMessage()

        removeEmbedFieldAndMessage(index, context, selectedMessage, modularMessage)

        messageWrapper.setMessage(context.guildId, selectedMessage, modularMessage)
    }

    private suspend fun removeEmbedFieldAndMessage(
        index: Int,
        context: ICommandContext,
        msgName: String,
        modularMessage: ModularMessage
    ) {
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
            .withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    suspend fun showEmbedFields(context: ICommandContext) {
        val messageWrapper = context.daoManager.messageWrapper
        val selectedMessage = getSelectedMessage(context) ?: return
        val modularMessage = messageWrapper.getMessage(context.guildId, selectedMessage)
            ?: ModularMessage()

        showEmbedFieldsAndMessage(context, selectedMessage, modularMessage)
    }

    private suspend fun showEmbedFieldsAndMessage(
        context: ICommandContext,
        msgName: String,
        modularMessage: ModularMessage
    ) {
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
        }.withVariable(PLACEHOLDER_TYPE, msgName)

        sendRsp(context, msg)
    }

    suspend fun showMessagePreviewTyped(context: ICommandContext, msgName: String) {
        val guildId = context.guildId
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.getMessage(guildId, msgName) ?: return
        sendModularMessagePreview(context, modularMessage, msgName)
    }

    suspend fun sendModularMessagePreview(
        context: ICommandContext,
        modularMessage: ModularMessage,
        msgName: String
    ) {
        val message = try {
            val msg = replaceUrlVariablesInPreview(context.member, modularMessage).toMessage()
            if (msg == null && modularMessage.attachments.isEmpty()) {
                val msg2 = context.getTranslation("message.view.isempty")
                    .withVariable("msgName", msgName)
                sendRsp(context, msg2)
                return
            }
            msg
        } catch (t: Throwable) {
            when (t) {
                is UserFriendlyException -> sendRsp(context, t.getUserFriendlyMessage())
                else -> throw t
            }
            return
        }

        val httpClient = context.webManager.proxiedHttpClient
        val channel = context.textChannel
        when {
            message == null -> sendAttachments(channel, httpClient, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachments(
                channel,
                httpClient,
                message,
                modularMessage.attachments
            )
            else -> sendMsg(channel, message)
        }
    }

    private suspend fun replaceUrlVariablesInPreview(
        member: Member,
        modularMessage: ModularMessage
    ): ModularMessage {
        val args = UrlParserArgs(member.guild, member.user)
        return modularMessage.mapAllStringFieldsSafe {
            if (it != null) {
                UrlJagTagParser.parseJagTag(args, it)
            } else {
                null
            }
        }
    }

    suspend fun setPingable(context: ICommandContext, msgName: String, pingable: Boolean) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.getMessage(context.guildId, msgName)
            ?: ModularMessage()

        setPingableAndMessage(context, modularMessage, msgName, pingable)
        messageWrapper.setMessage(context.guildId, msgName, modularMessage)
    }

    private suspend fun setPingableAndMessage(
        context: ICommandContext,
        message: ModularMessage,
        msgName: String,
        pingable: Boolean
    ) {
        val mutableMap = message.extra.toMutableMap()
        if (pingable) {
            mutableMap["isPingable"] = ""
        } else {
            mutableMap.remove("isPingable")
        }

        message.extra = mutableMap

        val msg = context.getTranslation("message.pingable.set.$pingable")
            .withVariable(PLACEHOLDER_TYPE, msgName)
        sendRsp(context, msg)
    }

    suspend fun showPingable(context: ICommandContext, msgName: String) {
        val guildId = context.guildId
        val messageWrapper = context.daoManager.messageWrapper
        val message = messageWrapper.getMessage(guildId, msgName)
            ?: ModularMessage()
        val isPingable = message.extra.containsKey("isPingable")

        val msg = context.getTranslation("message.pingable.show.$isPingable")
            .withVariable(PLACEHOLDER_TYPE, msgName)
        sendRsp(context, msg)
    }
}