package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*

class CustomCommandCommand : AbstractCommand("command.customcommand") {

    init {
        id = 36
        name = "customCommand"
        aliases = arrayOf("cc")
        children = arrayOf(
            ListArg(root),
            AddArg(root),
            AliasesArg(root),
            RemoveArg(root),
            SelectArg(root),
            SetChanceArg(root),
            SetPrefixStateArg(root),
            SetDescriptionArg(root),
            ResponseArg(root),
            InfoArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION

    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, Long>()
        suspend fun getSelectedCCNMessage(context: CommandContext): CustomCommand? {
            val language = context.getLanguage()
            val pair = Pair(context.getGuildId(), context.authorId)
            return if (selectionMap.containsKey(pair)) {
                val id = selectionMap[pair]
                val ccs = context.daoManager.customCommandWrapper.customCommandCache.get(context.getGuildId()).await()
                    .filter { cc -> cc.id == id }
                if (ccs.isNotEmpty()) {
                    ccs[0]
                } else {
                    val msg = i18n.getTranslation(language, "message.ccremoved")
                        .replace(PREFIX_PLACE_HOLDER, context.usedPrefix)
                    sendMsg(context, msg)
                    null
                }
            } else {
                val msg = i18n.getTranslation(language, "message.noccselected")
                    .replace(PREFIX_PLACE_HOLDER, context.usedPrefix)
                sendMsg(context, msg)
                null
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class InfoArg(root: String) : AbstractCommand("$root.info") {

        init {
            name = "info"
            aliases = arrayOf("information")
        }

        override suspend fun execute(context: CommandContext) {
            val id = getLongFromArgN(context, 0)
            var cc = context.daoManager.customCommandWrapper.getCCById(context.getGuildId(), id)
            if (cc == null && context.args.isNotEmpty()) {

                val msg = i18n.getTranslation(context, "message.unknown.ccid")
                    .replace(PLACEHOLDER_ARG, id.toString())
                sendMsg(context, msg)
                return
            } else if (cc == null) {
                cc = getSelectedCCNMessage(context) ?: return
            }

            val title = i18n.getTranslation(context, "$root.title")
            val description = i18n.getTranslation(context, "$root.description")
                .replace("%ccName%", cc.name)
            val eb = Embedder(context)
            eb.setTitle(title)
            eb.setDescription(description)
            sendEmbed(context, eb.build())
        }
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val language = context.getLanguage()
            val title = i18n.getTranslation(language, "$root.title")

            val ccs = context.daoManager.customCommandWrapper.customCommandCache.get(context.getGuildId()).await()
            var content = "```INI"

            content += "\n[id] - [name] - [chance]"
            for (cc in ccs) {
                content += "\n[${cc.id}] - ${cc.name} - ${cc.chance}"
            }
            content += "```"

            val msg = title + content
            sendMsgCodeBlock(context, msg, "INI")
        }
    }

    class AddArg(root: String) : AbstractCommand("$root.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            val args = context.rawArg.split("\\s*>\\s*".toRegex())
            if (args.size < 2) {
                sendSyntax(context, syntax)
                return
            }


            val name = args[0]
            val content = if (args[1].isBlank()) {
                "empty"
            } else {
                args[1]
            }
            val cc = CustomCommand(0, name, ModularMessage(content))

            val ccId = context.daoManager.customCommandWrapper.add(context.getGuildId(), cc)
            cc.id = ccId

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
                .replace("%id%", cc.id.toString())
                .replace("%ccName%", cc.name)
                .replace("%content%", cc.content.messageContent ?: "error")
            sendMsg(context, msg)
        }

    }

    class RemoveArg(root: String) : AbstractCommand("$root.remove") {

        init {
            name = "remove"
            aliases = arrayOf("delete", "rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }
            val guildId = context.getGuildId()

            val id = getLongFromArgNMessage(context, 0) ?: return
            val cc = context.daoManager.customCommandWrapper.customCommandCache.get(guildId).await()
                .first { cc -> cc.id == id }

            context.daoManager.customCommandWrapper.remove(guildId, id)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
                .replace("%id%", cc.id.toString())
                .replace("%ccName%", cc.name)

            sendMsg(context, msg)
        }

    }

    class SelectArg(root: String) : AbstractCommand("$root.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }
            val guildId = context.getGuildId()

            val id = getLongFromArgNMessage(context, 0) ?: return
            val cc = context.daoManager.customCommandWrapper.getCCById(guildId, id)
            if (cc == null) {
                val msg = i18n.getTranslation(context, "message.unknown.ccid")
                    .replace(PLACEHOLDER_ARG, id.toString())
                sendMsg(context, msg)
                return
            }

            selectionMap[Pair(guildId, context.authorId)] = id


            val msg = i18n.getTranslation(context, "$root.selected")
                .replace("%id%", cc.id.toString())
                .replace("%ccName%", cc.name)
            sendMsg(context, msg)

        }
    }


    class AliasesArg(root: String) : AbstractCommand("$root.aliases") {

        init {
            name = "aliases"
            children = arrayOf(AddArg(root), RemoveArg(root), ListArg(root))
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class AddArg(root: String) : AbstractCommand("$root.add") {

            init {
                name = "add"
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val ccSelected = getSelectedCCNMessage(context) ?: return
                val s = ccSelected.aliases?.toMutableList() ?: mutableListOf()
                s.add(context.rawArg)
                ccSelected.aliases = s.toList()


                context.daoManager.customCommandWrapper.update(context.getGuildId(), ccSelected)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.success")
                    .replace("%id%", ccSelected.id.toString())
                    .replace("%ccName%", ccSelected.name)
                    .replace(PLACEHOLDER_ARG, context.rawArg)

                sendMsg(context, msg)
            }

        }

        class RemoveArg(root: String) : AbstractCommand("$root.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm", "rem", "delete")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val ccSelected = getSelectedCCNMessage(context) ?: return
                val s = ccSelected.aliases?.toMutableList() ?: mutableListOf()
                val possibleLong = getIntegerFromArgN(context, 0) ?: s.indexOf(context.rawArg)

                val alias: String

                if (possibleLong == -1) {
                    sendSyntax(context, syntax)
                    return
                } else {
                    alias = s[possibleLong]
                    s.removeAt(possibleLong)
                }
                ccSelected.aliases = s.toList()


                context.daoManager.customCommandWrapper.update(context.getGuildId(), ccSelected)

                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "$root.success")
                    .replace("%id%", ccSelected.id.toString())
                    .replace("%ccName%", ccSelected.name)
                    .replace("%position%", possibleLong.toString())
                    .replace("%alias%", alias)

                sendMsg(context, msg)
            }

        }

        class ListArg(root: String) : AbstractCommand("$root.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context, syntax)
                    return
                }

                val ccSelected = getSelectedCCNMessage(context) ?: return
                val aliases = ccSelected.aliases
                val language = context.getLanguage()

                val path = if (aliases == null) "$root.empty" else "$root.title"
                val title = i18n.getTranslation(language, path)
                    .replace("%id%", ccSelected.id.toString())
                    .replace("%ccName%", ccSelected.name)

                val content = if (aliases == null) {
                    ""
                } else {
                    var build = "```INI"
                    for ((index, alias) in aliases.withIndex()) {
                        build += "\n[$index] - $alias"
                    }
                    "$build```"
                }

                val msg = title + content

                sendMsg(context, msg)
            }

        }

    }

    class SetDescriptionArg(root: String) : AbstractCommand("$root.setdescription") {

        init {
            name = "setDescription"
            aliases = arrayOf("setDesc", "sd")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }

            val ccSelected = getSelectedCCNMessage(context) ?: return
            ccSelected.description = context.rawArg

            context.daoManager.customCommandWrapper.update(context.getGuildId(), ccSelected)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
                .replace("%id%", ccSelected.id.toString())
                .replace("%ccName%", ccSelected.name)
                .replace(PLACEHOLDER_ARG, context.rawArg)

            sendMsg(context, msg)
        }
    }

    class SetChanceArg(root: String) : AbstractCommand("$root.setchance") {

        init {
            name = "setChance"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }

            val ccSelected = getSelectedCCNMessage(context) ?: return
            val chance = getIntegerFromArgNMessage(context, 0) ?: return
            ccSelected.chance = chance

            context.daoManager.customCommandWrapper.update(context.getGuildId(), ccSelected)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
                .replace("%id%", ccSelected.id.toString())
                .replace("%ccName%", ccSelected.name)
                .replace(PLACEHOLDER_ARG, chance.toString())

            sendMsg(context, msg)
        }

    }

    class SetPrefixStateArg(root: String) : AbstractCommand("$root.setprefixstate") {

        init {
            name = "setPrefixState"
            aliases = arrayOf("sps")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }

            val ccSelected = getSelectedCCNMessage(context) ?: return
            val state = getBooleanFromArgNMessage(context, 0) ?: return
            ccSelected.prefix = state


            context.daoManager.customCommandWrapper.update(context.getGuildId(), ccSelected)

            val language = context.getLanguage()
            val pathPart = if (state) "enabled" else "disabled"
            val msg = i18n.getTranslation(language, "$root.$pathPart")
                .replace("%id%", ccSelected.id.toString())
                .replace("%ccName%", ccSelected.name)

            sendMsg(context, msg)
        }

    }

    class ResponseArg(root: String) : AbstractCommand("$root.response") {

        init {
            name = "response"
            aliases = arrayOf("r")
            children = arrayOf(
                SetContentArg(this.root),
                EmbedArg(this.root),
                AttachmentsArg(this.root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context, syntax)
        }

        class SetContentArg(root: String) : AbstractCommand("$root.setcontent") {

            init {
                name = "setContent"
                aliases = arrayOf("sc")
            }

            override suspend fun execute(context: CommandContext) {
                val cc = getSelectedCCNMessage(context) ?: return
                val property = ModularMessageProperty.CONTENT
                if (context.args.isEmpty()) {
                    MessageCommandUtil.showMessageCC(context, property, cc)
                } else {
                    MessageCommandUtil.setMessageCC(context, property, cc)
                }
            }
        }

        class EmbedArg(root: String) : AbstractCommand("$root.embed") {

            init {
                name = "embed"
                aliases = arrayOf("e")
                children = arrayOf(
                    ClearArg(this.root),
                    SetDescriptionArg(this.root),
                    SetColorArg(this.root),
                    SetTitleArg(this.root),
                    SetTitleUrlArg(this.root),
                    SetAuthorArg(this.root),
                    SetAuthorIconArg(this.root),
                    SetAuthorUrlArg(this.root),
                    SetThumbnailArg(this.root),
                    SetImageArg(this.root),
                    FieldArg(this.root),
                    SetFooterArg(this.root),
                    SetFooterIconArg(this.root)
                    //What even is optimization
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context, syntax)
            }

            class SetTitleArg(root: String) : AbstractCommand("$root.settitle") {

                init {
                    name = "setTitle"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_TITLE
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetTitleUrlArg(root: String) : AbstractCommand("$root.settitleurl") {

                init {
                    name = "setTitleUrl"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }


            class SetAuthorArg(root: String) : AbstractCommand("$root.setauthor") {

                init {
                    name = "setAuthor"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetAuthorIconArg(root: String) : AbstractCommand("$root.setauthoricon") {

                init {
                    name = "setAuthorIcon"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_ICON_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetAuthorUrlArg(root: String) : AbstractCommand("$root.setauthorurl") {

                init {
                    name = "setAuthorUrl"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_AUTHOR_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }


            class SetThumbnailArg(root: String) : AbstractCommand("$root.setthumbnail") {

                init {
                    name = "setThumbnail"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_THUMBNAIL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetImageArg(root: String) : AbstractCommand("$root.setimage") {

                init {
                    name = "setImage"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_IMAGE
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }


            class FieldArg(root: String) : AbstractCommand("$root.field") {

                init {
                    name = "field"
                    children = arrayOf(
                        AddArg(this.root),
                        RemoveArg(this.root),
                        ListArg(this.root),
                        SetTitleArg(this.root),
                        SetValueArg(this.root),
                        SetInlineArg(this.root)
                    )
                }

                override suspend fun execute(context: CommandContext) {
                    sendSyntax(context, syntax)
                }

                class AddArg(root: String) : AbstractCommand("$root.add") {

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
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.addEmbedFieldCC(title, value, inline, context, cc)
                    }
                }

                class SetTitleArg(root: String) : AbstractCommand("$root.settitle") {

                    init {
                        name = "setTitle"
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context, syntax)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val title = context.rawArg
                            .replaceFirst("$index", "")
                            .trim()
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldTitleCC(index, title, context, cc)
                    }
                }

                class SetValueArg(root: String) : AbstractCommand("$root.setvalue") {

                    init {
                        name = "setValue"
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.size < 2) {
                            sendSyntax(context, syntax)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val value = context.rawArg
                            .replaceFirst("$index", "")
                            .trim()
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldValueCC(index, value, context, cc)
                    }
                }

                class SetInlineArg(root: String) : AbstractCommand("$root.setinline") {

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
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldInlineCC(index, value, context, cc)
                    }
                }

                class RemoveArg(root: String) : AbstractCommand("$root.remove") {

                    init {
                        name = "remove"
                        aliases = arrayOf("rm", "rem", "delete")
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.isEmpty()) {
                            sendSyntax(context, syntax)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.removeEmbedFieldCC(index, context, cc)
                    }
                }

                class ListArg(root: String) : AbstractCommand("$root.list") {

                    init {
                        name = "list"
                        aliases = arrayOf("ls")
                    }

                    override suspend fun execute(context: CommandContext) {
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.showEmbedFieldsCC(context, cc)
                    }
                }
            }


            class SetDescriptionArg(root: String) : AbstractCommand("$root.setdescription") {

                init {
                    name = "setDescription"
                    aliases = arrayOf("setDesc")
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_DESCRIPTION
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetColorArg(root: String) : AbstractCommand("$root.setcolor") {

                init {
                    name = "setColor"
                    aliases = arrayOf("setColour")
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_COLOR
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetFooterArg(root: String) : AbstractCommand("$root.setfooter") {

                init {
                    name = "setFooter"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_FOOTER
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class SetFooterIconArg(root: String) : AbstractCommand("$root.setfootericon") {

                init {
                    name = "setFooterIcon"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_FOOTER_ICON_URL
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class ClearArg(root: String) : AbstractCommand("$root.clear") {

                init {
                    name = "clear"
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.clearEmbedCC(context, cc)
                }
            }
        }

        class AttachmentsArg(root: String) : AbstractCommand("$root.attachments") {

            init {
                name = "attachments"
                aliases = arrayOf("a")
                children = arrayOf(
                    ListArg(this.root),
                    AddArg(this.root),
                    RemoveArg(this.root)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context, syntax)
            }

            class ListArg(root: String) : AbstractCommand("$root.list") {

                init {
                    name = "list"
                    arrayOf("ls")
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.listAttachmentsCC(context, cc)
                }
            }

            class AddArg(root: String) : AbstractCommand("$root.add") {

                init {
                    name = "add"
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    if (context.args.isEmpty()) {
                        sendSyntax(context, syntax)
                        return
                    }
                    MessageCommandUtil.addAttachmentCC(context, cc)
                }

            }

            class RemoveArg(root: String) : AbstractCommand("$root.remove") {

                init {
                    name = "remove"
                    aliases = arrayOf("rm", "rem", "delete")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context, syntax)
                        return
                    }
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.removeAttachmentCC(context, cc)
                }
            }

        }

    }
}