package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commandutil.administration.MessageCommandUtil
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ModularMessageProperty
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresGuildPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax

const val CUSTOM_COMMAND_LIMIT = 10
const val PREMIUM_CUSTOM_COMMAND_LIMIT = 100
const val PREMIUM_CC_LIMIT_PATH = "premium.feature.cc.limit"

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
            RenameArg(root),
            CopyArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class CopyArg(parent: String) : AbstractCommand("$parent.copy") {

        init {
            name = "copy"
            aliases = arrayOf("cp")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val guildId = context.guildId

            val id1 = getLongFromArgNMessage(context, 0) ?: return
            val id2 = getLongFromArgNMessage(context, 1) ?: return
            val cc1 = context.daoManager.customCommandWrapper.getCCById(guildId, id1)
            val cc2 = context.daoManager.customCommandWrapper.getCCById(guildId, id2)
            if (cc1 == null) {
                val msg = context.getTranslation("message.unknown.ccid")
                    .withVariable(PLACEHOLDER_ARG, id.toString())
                sendRsp(context, msg)
                return
            }
            if (cc2 == null) {
                val msg = context.getTranslation("message.unknown.ccid")
                    .withVariable(PLACEHOLDER_ARG, id.toString())
                sendRsp(context, msg)
                return
            }

            cc2.chance = cc1.chance
            cc2.content = cc1.content
            cc2.prefix = cc1.prefix

            context.daoManager.customCommandWrapper.update(context.guildId, cc2)

            val msg = context.getTranslation("$root.success")
                .withVariable("id1", cc1.id)
                .withVariable("ccName1", cc1.name)
                .withVariable("id2", cc2.id)
                .withVariable("ccName2", cc2.name)
            sendRsp(context, msg)
        }
    }

    class RenameArg(parent: String) : AbstractCommand("$parent.rename") {

        init {
            name = "rename"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val cc = getSelectedCCNMessage(context) ?: return
            val newName = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
            val oldName = cc.name
            cc.name = newName
            context.daoManager.customCommandWrapper.update(context.guildId, cc)

            val msg = context.getTranslation("$root.success")
                .withVariable("oldName", oldName)
                .withVariable("newName", newName)
            sendRsp(context, msg)
        }
    }

    companion object {
        val selectionMap = HashMap<Pair<Long, Long>, Long>()
        suspend fun getSelectedCCNMessage(context: CommandContext): CustomCommand? {
            val pair = Pair(context.guildId, context.authorId)
            return if (selectionMap.containsKey(pair)) {
                val id = selectionMap[pair]
                val cc = context.daoManager.customCommandWrapper.getCCById(context.guildId, id)
                if (cc == null) {
                    val msg = context.getTranslation("message.ccremoved")
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                    sendRsp(context, msg)
                }
                cc
            } else {
                val msg = context.getTranslation("message.noccselected")
                    .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
                null
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val title = context.getTranslation("$root.title")

            val ccs = context.daoManager.customCommandWrapper.getList(context.guildId)
            var content = "```INI"

            content += "\n[id] - [name] - [chance]"
            for (cc in ccs) {
                content += "\n[${cc.id}] - ${cc.name} - ${cc.chance}"
            }
            content += "```"

            val msg = title + content
            sendRspCodeBlock(context, msg, "INI")
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.customCommandWrapper
            val size = wrapper.getList(context.guildId).size
            if (size >= CUSTOM_COMMAND_LIMIT && !isPremiumGuild(context)) {
                sendFeatureRequiresGuildPremiumMessage(context, PREMIUM_CC_LIMIT_PATH)
                return
            }

            if (size >= PREMIUM_CUSTOM_COMMAND_LIMIT) {
                val msg = context.getTranslation("$root.limit")
                    .withVariable("limit", PREMIUM_CUSTOM_COMMAND_LIMIT.toString())
                sendRsp(context, msg)
            }

            val name = context.args[0]
            var content = context.rawArg.removeFirst(name).trim()
            if (content.isBlank()) content = "empty"

            val cc = CustomCommand(0, name, ModularMessage(content))

            val ccId = context.daoManager.customCommandWrapper.add(context.guildId, cc)
            cc.id = ccId

            val msg = context.getTranslation("$root.success")
                .withVariable("id", cc.id.toString())
                .withVariable("ccName", cc.name)
                .withVariable("content", cc.content.messageContent ?: "error")
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("delete", "rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val guildId = context.guildId

            val id = getLongFromArgNMessage(context, 0) ?: return
            val cc = context.daoManager.customCommandWrapper.getList(guildId)
                .firstOrNull { (ccId) -> ccId == id }

            if (cc == null) {
                val msg = context.getTranslation("$root.failed")
                    .withVariable("id", id.toString())
                    .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
                return
            }

            context.daoManager.customCommandWrapper.remove(guildId, id)

            val msg = context.getTranslation("$root.success")
                .withVariable("id", cc.id.toString())
                .withVariable("ccName", cc.name)

            sendRsp(context, msg)
        }
    }

    class SelectArg(parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val guildId = context.guildId

            val id = getLongFromArgNMessage(context, 0) ?: return
            val cc = context.daoManager.customCommandWrapper.getCCById(guildId, id)
            if (cc == null) {
                val msg = context.getTranslation("message.unknown.ccid")
                    .withVariable(PLACEHOLDER_ARG, id.toString())
                sendRsp(context, msg)
                return
            }

            selectionMap[guildId to context.authorId] = id


            val msg = context.getTranslation("$root.selected")
                .withVariable("id", cc.id.toString())
                .withVariable("ccName", cc.name)
            sendRsp(context, msg)

        }
    }


    class AliasesArg(parent: String) : AbstractCommand("$parent.aliases") {

        init {
            name = "aliases"
            children = arrayOf(
                AddArg(root),
                RemoveArg(root),
                ListArg(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class AddArg(root: String) : AbstractCommand("$root.add") {

            init {
                name = "add"
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val ccSelected = getSelectedCCNMessage(context) ?: return
                val s = ccSelected.aliases?.toMutableList() ?: mutableListOf()
                s.add(context.rawArg)
                ccSelected.aliases = s.toList()

                context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

                val msg = context.getTranslation("$root.success")
                    .withVariable("id", ccSelected.id.toString())
                    .withVariable("ccName", ccSelected.name)
                    .withVariable(PLACEHOLDER_ARG, context.rawArg)

                sendRsp(context, msg)
            }
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm", "rem", "delete")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val ccSelected = getSelectedCCNMessage(context) ?: return
                val s = ccSelected.aliases?.toMutableList() ?: mutableListOf()
                val possibleLong = getIntegerFromArgN(context, 0) ?: s.indexOf(context.rawArg)

                val alias: String

                if (possibleLong == -1) {
                    sendSyntax(context)
                    return
                } else {
                    alias = s[possibleLong]
                    s.removeAt(possibleLong)
                }
                ccSelected.aliases = s.toList()

                context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

                val msg = context.getTranslation("$root.success")
                    .withVariable("id", ccSelected.id.toString())
                    .withVariable("ccName", ccSelected.name)
                    .withVariable("position", possibleLong.toString())
                    .withVariable(PLACEHOLDER_ARG, alias)

                sendRsp(context, msg)
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val ccSelected = getSelectedCCNMessage(context) ?: return
                val aliases = ccSelected.aliases

                val path = if (aliases == null) "$root.empty" else "$root.title"
                val title = context.getTranslation(path)
                    .withVariable("id", ccSelected.id.toString())
                    .withVariable("ccName", ccSelected.name)

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

                sendRsp(context, msg)
            }
        }
    }

    class SetDescriptionArg(parent: String) : AbstractCommand("$parent.setdescription") {

        init {
            name = "setDescription"
            aliases = arrayOf("setDesc", "sd")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val unset = context.rawArg == "null"
            val ccSelected = getSelectedCCNMessage(context) ?: return
            ccSelected.description = if (context.rawArg == "null") null else context.rawArg

            context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

            val msg = context.getTranslation("$root.${if (unset) "unset" else "set"}")
                .withVariable("id", ccSelected.id.toString())
                .withVariable("ccName", ccSelected.name)
                .withVariable(PLACEHOLDER_ARG, context.rawArg)

            sendRsp(context, msg)
        }
    }

    class SetChanceArg(parent: String) : AbstractCommand("$parent.setchance") {

        init {
            name = "setChance"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val ccSelected = getSelectedCCNMessage(context) ?: return
            val chance = getIntegerFromArgNMessage(context, 0) ?: return
            ccSelected.chance = chance

            context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

            val msg = context.getTranslation("$root.success")
                .withVariable("id", ccSelected.id.toString())
                .withVariable("ccName", ccSelected.name)
                .withVariable(PLACEHOLDER_ARG, chance.toString())

            sendRsp(context, msg)
        }

    }

    class SetPrefixStateArg(parent: String) : AbstractCommand("$parent.setprefixstate") {

        init {
            name = "setPrefixState"
            aliases = arrayOf("sps")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val ccSelected = getSelectedCCNMessage(context) ?: return
            val state = getBooleanFromArgNMessage(context, 0) ?: return
            ccSelected.prefix = state


            context.daoManager.customCommandWrapper.update(context.guildId, ccSelected)

            val pathPart = if (state) "enabled" else "disabled"
            val msg = context.getTranslation("$root.$pathPart")
                .withVariable("id", ccSelected.id.toString())
                .withVariable("ccName", ccSelected.name)

            sendRsp(context, msg)
        }

    }

    class ResponseArg(parent: String) : AbstractCommand("$parent.response") {

        init {
            name = "response"
            aliases = arrayOf("r")
            children = arrayOf(
                SetContentArg(root),
                EmbedArg(root),
                AttachmentsArg(root),
                ViewArg(root),
                SetPingableArg(root)
            )
        }

        class SetPingableArg(parent: String) : AbstractCommand("$parent.setpingable") {

            init {
                name = "setPingable"
            }

            override suspend fun execute(context: CommandContext) {
                val cc = getSelectedCCNMessage(context) ?: return
                if (context.rawArg.isBlank()) {
                    MessageCommandUtil.showPingableCC(context, cc)
                    return
                }

                val pingable = getBooleanFromArgNMessage(context, 0) ?: return
                MessageCommandUtil.setPingableCC(context, cc, pingable)
            }
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }

        class ViewArg(parent: String) : AbstractCommand("$parent.view") {
            init {
                name = "view"
                aliases = arrayOf("preview", "show", "info")
            }

            override suspend fun execute(context: CommandContext) {
                val cc = getSelectedCCNMessage(context) ?: return
                MessageCommandUtil.showMessagePreviewCC(context, cc)
            }
        }

        class SetContentArg(parent: String) : AbstractCommand("$parent.setcontent") {

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

        class EmbedArg(parent: String) : AbstractCommand("$parent.embed") {

            init {
                name = "embed"
                aliases = arrayOf("e")
                children = arrayOf(
                    ClearArg(root),
                    SetDescriptionArg(root),
                    SetColorArg(root),
                    SetTitleArg(root),
                    SetTitleUrlArg(root),
                    SetAuthorArg(root),
                    SetAuthorIconArg(root),
                    SetAuthorUrlArg(root),
                    SetThumbnailArg(root),
                    SetImageArg(root),
                    FieldArg(root),
                    SetFooterArg(root),
                    SetFooterIconArg(root),
                    SetTimeStampArg(root)
                    //What even is optimization
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class SetTitleArg(parent: String) : AbstractCommand("$parent.settitle") {

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

            class SetTitleUrlArg(parent: String) : AbstractCommand("$parent.settitleurl") {

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


            class SetAuthorArg(parent: String) : AbstractCommand("$parent.setauthor") {

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

            class SetAuthorIconArg(parent: String) : AbstractCommand("$parent.setauthoricon") {

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

            class SetAuthorUrlArg(parent: String) : AbstractCommand("$parent.setauthorurl") {

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


            class SetThumbnailArg(parent: String) : AbstractCommand("$parent.setthumbnail") {

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

            class SetImageArg(parent: String) : AbstractCommand("$parent.setimage") {

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


            class FieldArg(parent: String) : AbstractCommand("$parent.field") {

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

                override suspend fun execute(context: CommandContext) {
                    sendSyntax(context)
                }

                class AddArg(parent: String) : AbstractCommand("$parent.add") {

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
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.addEmbedFieldCC(title, value, inline, context, cc)
                    }
                }

                class SetTitleArg(parent: String) : AbstractCommand("$parent.settitle") {

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
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldTitleCC(index, title, context, cc)
                    }
                }

                class SetValueArg(parent: String) : AbstractCommand("$parent.setvalue") {

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
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldValueCC(index, value, context, cc)
                    }
                }

                class SetInlineArg(parent: String) : AbstractCommand("$parent.setinline") {

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
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.setEmbedFieldInlineCC(index, value, context, cc)
                    }
                }

                class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                    init {
                        name = "remove"
                        aliases = arrayOf("rm", "rem", "delete")
                    }

                    override suspend fun execute(context: CommandContext) {
                        if (context.args.isEmpty()) {
                            sendSyntax(context)
                            return
                        }
                        val index = getIntegerFromArgNMessage(context, 0) ?: return
                        val cc = getSelectedCCNMessage(context) ?: return
                        MessageCommandUtil.removeEmbedFieldCC(index, context, cc)
                    }
                }

                class ListArg(parent: String) : AbstractCommand("$parent.list") {

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


            class SetDescriptionArg(parent: String) : AbstractCommand("$parent.setdescription") {

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

            class SetColorArg(parent: String) : AbstractCommand("$parent.setcolor") {

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

            class SetFooterArg(parent: String) : AbstractCommand("$parent.setfooter") {

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

            class SetFooterIconArg(parent: String) : AbstractCommand("$parent.setfootericon") {

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

            class SetTimeStampArg(parent: String) : AbstractCommand("$parent.settimestamp") {

                init {
                    name = "setTimeStamp"
                }

                override suspend fun execute(context: CommandContext) {
                    val property = ModularMessageProperty.EMBED_TIME_STAMP
                    val cc = getSelectedCCNMessage(context) ?: return
                    when {
                        context.rawArg.isBlank() -> MessageCommandUtil.showMessageCC(context, property, cc)
                        else -> MessageCommandUtil.setMessageCC(context, property, cc)
                    }
                }
            }

            class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

                init {
                    name = "clear"
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.clearEmbedCC(context, cc)
                }
            }
        }

        class AttachmentsArg(parent: String) : AbstractCommand("$parent.attachments") {

            init {
                name = "attachments"
                aliases = arrayOf("a")
                children = arrayOf(
                    ListArg(root),
                    AddArg(root),
                    RemoveArg(root)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class ListArg(parent: String) : AbstractCommand("$parent.list") {

                init {
                    name = "list"
                    arrayOf("ls")
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.listAttachmentsCC(context, cc)
                }
            }

            class AddArg(parent: String) : AbstractCommand("$parent.add") {

                init {
                    name = "add"
                }

                override suspend fun execute(context: CommandContext) {
                    val cc = getSelectedCCNMessage(context) ?: return
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }
                    MessageCommandUtil.addAttachmentCC(context, cc)
                }

            }

            class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                init {
                    name = "remove"
                    aliases = arrayOf("rm", "rem", "delete")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }
                    val cc = getSelectedCCNMessage(context) ?: return
                    MessageCommandUtil.removeAttachmentCC(context, cc)
                }
            }
        }
    }
}