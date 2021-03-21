package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
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
            SetContainsTriggersArg(root),
            LinkMessageArg(root),
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

        override suspend fun execute(context: ICommandContext) {
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
            cc2.msgName = cc1.msgName
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

        override suspend fun execute(context: ICommandContext) {
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
        suspend fun getSelectedCCNMessage(context: ICommandContext): CustomCommand? {
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

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val title = context.getTranslation("$root.title")

            val ccs = context.daoManager.customCommandWrapper.getList(context.guildId)
            if (ccs.isEmpty()) {
                val msg = context.getTranslation("$root.empty")
                sendRsp(context, msg)
                return
            }

            var content = "```INI"

            content += "\n[id] - [name] - [chance] - [msgName]"
            for (cc in ccs) {
                content += "\n[${cc.id}] - ${cc.name} - ${cc.chance} - [${cc.msgName}]"
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

        override suspend fun execute(context: ICommandContext) {
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

            val name = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
            var msgName = context.fullArg.removeFirst(name).trim()
            if (msgName.isBlank()) msgName = "nameplaceholder"

            val cc = CustomCommand(0, name, msgName)

            val ccId = wrapper.add(context.guildId, cc)
            cc.id = ccId
            msgName = "cc.$ccId"
            cc.msgName = msgName
            wrapper.update(context.guildId, cc)

            val msg = context.getTranslation("$root.success")
                .withVariable("id", cc.id.toString())
                .withVariable("ccName", cc.name)
                .withVariable("msgName", msgName)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("delete", "rm")
        }

        override suspend fun execute(context: ICommandContext) {
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

        override suspend fun execute(context: ICommandContext) {
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

        override suspend fun execute(context: ICommandContext) {
            sendSyntax(context)
        }

        class AddArg(root: String) : AbstractCommand("$root.add") {

            init {
                name = "add"
            }

            override suspend fun execute(context: ICommandContext) {
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

            override suspend fun execute(context: ICommandContext) {
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

            override suspend fun execute(context: ICommandContext) {
                val ccSelected = getSelectedCCNMessage(context) ?: return
                val aliases = ccSelected.aliases

                val path = if (aliases?.isEmpty() == true) "$root.empty" else "$root.title"
                val title = context.getTranslation(path)
                    .withVariable("id", ccSelected.id.toString())
                    .withVariable("ccName", ccSelected.name)

                val content = if (aliases == null || aliases.isEmpty()) {
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

        override suspend fun execute(context: ICommandContext) {
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

    class SetContainsTriggersArg(parent: String) : AbstractCommand("$parent.setcontainstriggers") {

        init {
            name = "setContainsTriggers"
            aliases = arrayOf("sct")
        }

        override suspend fun execute(context: ICommandContext) {
            val cc = getSelectedCCNMessage(context) ?: return
            if (context.args.isEmpty()) {
                val msg = context.getTranslation("$root.show.${cc.containsTriggers}")
                    .withVariable("id", cc.id.toString())
                    .withVariable("ccName", cc.name)
                sendRsp(context, msg)
                return
            }

            val state = getBooleanFromArgNMessage(context, 0) ?: return
            cc.containsTriggers = state
            context.daoManager.customCommandWrapper.update(context.guildId, cc)
            val msg = context.getTranslation("$root.set.${state}")
                .withVariable("id", cc.id.toString())
                .withVariable("ccName", cc.name)
            sendRsp(context, msg)
        }
    }

    class SetChanceArg(parent: String) : AbstractCommand("$parent.setchance") {

        init {
            name = "setChance"
            aliases = arrayOf("sc")
        }

        override suspend fun execute(context: ICommandContext) {
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

        override suspend fun execute(context: ICommandContext) {
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

    class LinkMessageArg(parent: String) : AbstractCommand("$parent.linkmessage") {

        init {
            name = "linkMessage"
            aliases = arrayOf("lm", "linkMsg")
        }

        override suspend fun execute(context: ICommandContext) {
            val id = getLongFromArgNMessage(context, 0) ?: return
            val cc = context.daoManager.customCommandWrapper.getCCById(context.guildId, id)
            if (cc == null) {
                val msg = context.getTranslation("message.unknown.ccid")
                    .withVariable(PLACEHOLDER_ARG, id.toString())
                sendRsp(context, msg)
                return
            }

            if (context.args.size == 1) {
                val msgName = cc.msgName
                if (msgName == null || msgName.isBlank()) {
                    val msg = context.getTranslation("$root.showempty")
                        .withVariable("ccName", cc.name)
                        .withVariable("id", cc.id)
                    sendRsp(context, msg)
                } else {
                    val msg = context.getTranslation("$root.show")
                        .withVariable("ccName", cc.name)
                        .withVariable("id", cc.id)
                        .withVariable("msgName", msgName)
                    sendRsp(context, msg)
                }
            } else {
                val msgName = getStringFromArgsNMessage(context, 1, 1, 64) ?: return
                val daoManager = context.daoManager
                if (msgName == "null") {
                    cc.msgName = null
                    daoManager.customCommandWrapper.update(context.guildId, cc)

                    val msg = context.getTranslation("$root.unlinked")
                        .withVariable("ccName", cc.name)
                        .withVariable("id", cc.id)
                    sendRsp(context, msg)
                } else {
                    val messages = daoManager.messageWrapper.getMessages(context.guildId)
                    if (msgName.isInside(messages, true)) {
                        cc.msgName = msgName
                        daoManager.customCommandWrapper.update(context.guildId, cc)

                        val msg = context.getTranslation("$root.linked")
                            .withVariable("ccName", cc.name)
                            .withVariable("id", cc.id)
                            .withVariable("msgName", msgName)
                        sendRsp(context, msg)
                    } else {
                        val msg = context.getTranslation("${context.commandOrder.first().root}.msgnoexist")
                            .withSafeVariable("msg", msgName)
                            .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                        sendRsp(context, msg)
                    }
                }
            }
        }
    }
}