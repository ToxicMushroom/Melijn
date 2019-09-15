package me.melijn.melijnbot.commandutil.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.message.MessageWrapper
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_TYPE
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.getColorFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.toHex
import net.dv8tion.jda.api.EmbedBuilder
import org.json.JSONObject

object MessageCommandUtil {

    suspend fun removeMessageIfEmpty(guildId: Long, type: MessageType, message: ModularMessage, messageWrapper: MessageWrapper): Boolean {
        return if (messageWrapper.shouldRemove(message)) {
            messageWrapper.removeMessage(guildId, type)
            true
        } else{
            false
        }
    }

    suspend fun setMessageContent(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()
        val arg = context.rawArg


        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeMessageContent(oldMessage, context.getGuildId(), type)
            Translateable("message.content.set.unset").string(context)
        } else {
            messageWrapper.setMessageContent(oldMessage, context.getGuildId(), type, arg)
            Translateable("message.content.set").string(context)
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showMessageContent(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val content = modularMessage?.messageContent
        val msg = if (content == null) {
            Translateable("message.content.show.unset").string(context)
        } else {
            Translateable("message.content.show.set").string(context)
                .replace("%content%", content)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedDescription(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val arg = context.rawArg


        val msg = if (arg.equals("null", true)) {
            if (oldMessage != null) {
                messageWrapper.removeEmbedDescription(oldMessage, context.getGuildId(), type)
            }
            Translateable("message.embed.description.unset").string(context)
        } else {
            messageWrapper.setEmbedDescription(oldMessage ?: ModularMessage(), context.getGuildId(), type, arg)
            Translateable("message.embed.description.set").string(context)
                .replace("%arg%", arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedDescription(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val description = modularMessage?.embed?.description
        val msg = if (description == null) {
            Translateable("message.embed.description.show.unset").string(context)
        } else {
            Translateable("message.embed.description.show.set").string(context)
                .replace("%content%", description)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun clearEmbed(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()

        if (modularMessage != null) {
            messageWrapper.clearEmbed(modularMessage, context.getGuildId(), type)
        }

        val msg = Translateable("message.embed.clear")
            .string(context)
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)

    }

    suspend fun listAttachments(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()

        val msg = if (modularMessage == null || modularMessage.attachments.isEmpty()) {
            Translateable("message.attachments.list.empty").string(context)
                .replace(PLACEHOLDER_TYPE, type.text)

        } else {
            val title = Translateable("message.attachments.list.title").string(context)
                .replace(PLACEHOLDER_TYPE, type.text)
            var content = "\n```INI"
            for ((index, attachment) in modularMessage.attachments.entries.withIndex()) {
                content += "\n$index - [${attachment.key}] - ${attachment.value}"
            }
            content += "```"
            (title + content)
        }
        sendMsg(context, msg)
    }

    suspend fun addAttachment(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val newMap = modularMessage.attachments.toMutableMap()
        newMap[context.args[0]] = context.args[1]

        modularMessage.attachments = newMap.toMap()

        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
        val msg = Translateable("message.attachments.add").string(context)
            .replace(PLACEHOLDER_TYPE, type.text)
            .replace("%attachment%", context.args[0])
            .replace("%file%", context.args[1])

        sendMsg(context, msg)
    }

    suspend fun removeAttachment(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val attachments = modularMessage.attachments.toMutableMap()
        val file = if (attachments.containsKey(context.args[0])) attachments[context.args[0]] else null
        attachments.remove(context.args[0])

        modularMessage.attachments = attachments.toMap()

        val msg =
            if (file == null) {
                Translateable("message.attachments.remove.notanattachment").string(context)

            } else {
                messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
                Translateable("message.attachments.remove.success").string(context)
                    .replace("%file%", file)
            }.replace(PLACEHOLDER_ARG, context.args[0])
                .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedColor(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val oldMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val color = oldMessage?.embed?.color

        val msg = if (color == null) {
            Translateable("message.embed.color.show.unset").string(context)
        } else {
            Translateable("message.embed.color.show.set").string(context)
                .replace("%color%", color.toHex())
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedColor(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedColor(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.color.unset").string(context)
        } else {
            val color = getColorFromArgNMessage(context, 0) ?: return
            messageWrapper.setEmbedColor(modularMessage, context.getGuildId(), type, color)
            Translateable("message.embed.color.set").string(context)
                .replace("%arg%", color.toHex())
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedTitle(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedTitleContent(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.title.unset").string(context)
        } else {
            messageWrapper.setEmbedTitleContent(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.title.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedTitle(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val title = modularMessage?.embed?.title

        val msg = if (title == null) {
            Translateable("message.embed.title.show.unset").string(context)
        } else {
            Translateable("message.embed.title.show.set").string(context)
                .replace("%title%", title)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedTitleUrl(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val url = modularMessage?.embed?.url

        val msg = if (url == null) {
            Translateable("message.embed.titleurl.show.unset").string(context)
        } else {
            Translateable("message.embed.titleurl.show.set").string(context)
                .replace("%url%", url)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedTitleUrl(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedTitleURL(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.titleurl.show.unset").string(context)
        } else {
            messageWrapper.setEmbedTitleURL(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.titleurl.show.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedAuthor(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val url = modularMessage?.embed?.author?.name

        val msg = if (url == null) {
            Translateable("message.embed.authorname.show.unset").string(context)
        } else {
            Translateable("message.embed.authorname.show.set").string(context)
                .replace("%name%", url)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedAuthor(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedAuthorContent(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.authorname.unset").string(context)
        } else {
            messageWrapper.setEmbedAuthorContent(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.authorname.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedAuthorIcon(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val url = modularMessage?.embed?.author?.iconUrl

        val msg = if (url == null) {
            Translateable("message.embed.authoricon.show.unset").string(context)
        } else {
            Translateable("message.embed.authoricon.show.set").string(context)
                .replace("%url", url)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedAuthorIcon(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedAuthorIconURL(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.authoricon.unset").string(context)
        } else {
            messageWrapper.setEmbedAuthorIconURL(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.authoricon.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedAuthorUrl(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val url = modularMessage?.embed?.author?.url

        val msg = if (url == null) {
            Translateable("message.embed.authorurl.show.unset").string(context)
        } else {
            Translateable("message.embed.authorurl.show.set").string(context)
                .replace("%url", url)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedAuthorUrl(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedAuthorURL(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.authorurl.unset").string(context)
        } else {
            messageWrapper.setEmbedAuthorURL(modularMessage, context.getGuildId(), type, context.rawArg)
            Translateable("message.embed.authorurl.set").string(context)
                .replace(PLACEHOLDER_ARG, context.rawArg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedThumbnail(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val url = modularMessage?.embed?.thumbnail?.url

        val msg = if (url == null) {
            Translateable("message.embed.thumbnail.show.unset").string(context)
        } else {
            Translateable("message.embed.thumbnail.show.set").string(context)
                .replace("%url", url)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedThumbnail(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedThumbnail(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.thumbnail.unset").string(context)
        } else {
            messageWrapper.setEmbedThumbnail(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.thumbnail.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedImage(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val url = modularMessage?.embed?.image?.url

        val msg = if (url == null) {
            Translateable("message.embed.image.show.unset").string(context)
        } else {
            Translateable("message.embed.image.show.set").string(context)
                .replace("%url", url)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedImage(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedImage(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.image.unset").string(context)
        } else {
            messageWrapper.setEmbedImage(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.image.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedFooter(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val footer = modularMessage?.embed?.footer?.text

        val msg = if (footer == null) {
            Translateable("message.embed.footer.show.unset").string(context)
        } else {
            Translateable("message.embed.footer.show.set").string(context)
                .replace("%url", footer)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedFooter(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedFooterContent(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.footer.unset").string(context)
        } else {
            messageWrapper.setEmbedFooterContent(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.footer.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedFooterIcon(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val footerUrl = modularMessage?.embed?.footer?.iconUrl

        val msg = if (footerUrl == null) {
            Translateable("message.embed.footericon.show.unset").string(context)
        } else {
            Translateable("message.embed.footericon.show.set").string(context)
                .replace("%url", footerUrl)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedFooterIcon(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val arg = context.rawArg

        val msg = if (arg.equals("null", true)) {
            messageWrapper.removeEmbedFooterURL(modularMessage, context.getGuildId(), type)
            Translateable("message.embed.footericon.show.unset").string(context)
        } else {
            messageWrapper.setEmbedFooterURL(modularMessage, context.getGuildId(), type, arg)
            Translateable("message.embed.footericon.show.set").string(context)
                .replace(PLACEHOLDER_ARG, arg)
        }.replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun addEmbedField(title: String, value: String, inline: Boolean, context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()
        val embed = modularMessage.embed ?: EmbedBuilder().build()
        val embedBuilder = EmbedBuilder(embed)
        embedBuilder.addField(title, value, inline)
        modularMessage.embed = embedBuilder.build()

        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
        val inlineString = Translateable(if (inline) "yes" else "no").string(context)
        val msg = Translateable("message.embed.field.add").string(context)
            .replace("%title%", title)
            .replace("%value%", value)
            .replace("%inline%", inlineString)
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun setEmbedFieldTitle(index: Int, title: String, context: CommandContext, type: MessageType) {
        setEmbedFieldPart(index, "title", title, context, type)
    }

    suspend fun setEmbedFieldValue(index: Int, value: String, context: CommandContext, type: MessageType) {
        setEmbedFieldPart(index, "value", value, context, type)
    }

    suspend fun setEmbedFieldInline(index: Int, inline: Boolean, context: CommandContext, type: MessageType) {
        setEmbedFieldPart(index, "inline", inline, context, type)
    }

    suspend fun setEmbedFieldPart(index: Int, partName: String, value: Any, context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        var modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
            ?: ModularMessage()

        val json = JSONObject(modularMessage.toJSON())
        val embedJSON = json.getJSONObject("embed")
        val fieldsJSON = embedJSON.getJSONArray("fields")
        val field = fieldsJSON.getJSONObject(index)
        field.put(partName, value)
        fieldsJSON.put(index, field)
        embedJSON.put("fields", fieldsJSON)
        json.put("embed", embedJSON)
        modularMessage = ModularMessage.fromJSON(json.toString(4))

        messageWrapper.setMessage(context.getGuildId(), type, modularMessage)
        val partValue: String = when (value) {
            is Boolean -> Translateable(if (value) "yes" else "no").string(context)
            else -> value.toString()
        }
        val msg = Translateable("message.embed.field$partName.set").string(context)
            .replace("%index%", index.toString())
            .replace("%$partName%", partValue)
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun removeEmbedField(index: Int, context: CommandContext, type: MessageType) {
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
        val msg = Translateable("message.embed.field.removed").string(context)
            .replace("%index%", index.toString())
            .replace(PLACEHOLDER_TYPE, type.text)

        sendMsg(context, msg)
    }

    suspend fun showEmbedFields(context: CommandContext, type: MessageType) {
        val messageWrapper = context.daoManager.messageWrapper
        val modularMessage = messageWrapper.messageCache.get(Pair(context.getGuildId(), type)).await()
        val fields = modularMessage?.embed?.fields

        val msg = if (fields == null || fields.isEmpty()) {
            Translateable("message.embed.field.list.empty").string(context)
        } else {
            val title = Translateable("message.embed.field.list.title").string(context)
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