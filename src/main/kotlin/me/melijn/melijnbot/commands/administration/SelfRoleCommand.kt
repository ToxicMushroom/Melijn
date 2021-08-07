package me.melijn.melijnbot.commands.administration

import com.freya02.emojis.Emojis
import me.melijn.melijnbot.database.role.SelfRoleGroup
import me.melijn.melijnbot.database.statesync.getEmote
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.internal.entities.EmoteImpl
import net.dv8tion.jda.internal.entities.GuildImpl

// const val UNKNOWN_SELFROLEMODE_PATH = "message.unknown.selfrolemode"

class SelfRoleCommand : AbstractCommand("command.selfrole") {

    init {
        id = 37
        name = "selfRole"
        aliases = arrayOf("sr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            SendGroupAutoArg(root),
            ClearArg(root),
            ListArg(root),
            GroupArg(root),
            AddReactionsToMessage(root),
            SetGetAllRolesArg(root),
            SetNameArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class AddReactionsToMessage(parent: String) : AbstractCommand("$parent.addreactionstomessage") {

        init {
            name = "addReactionsToMessage"
            aliases = arrayOf("artm")
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 2)) return

            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
            val channel = getTextChannelByArgsNMessage(context, 1) ?: return
            val messageId = getLongFromArgNMessage(context, 2) ?: return


            val selfRoles = context.daoManager.selfRoleWrapper.getMap(context.guildId)[group.groupName]
            val range = if (context.args.size > 4) {
                getIntegersFromArgsNMessage(context, 4, 1, selfRoles?.length() ?: 1) ?: return
            } else Array(selfRoles?.length() ?: 0) { i -> i }.toIntArray()

            val message = channel.retrieveMessageById(messageId).awaitOrNull()
            if (message == null) {
                sendRsp(context, "**$messageId** doesn't exist")
                return
            }

            val appendMessageId = (if (context.args.size > 3) {
                getBooleanFromArgNMessage(context, 3) ?: return
            } else false) && (!group.messageIds.contains(message.idLong) || group.channelId != channel.idLong)

            if (selfRoles == null || selfRoles.isEmpty) {
                val msg = context.getTranslation("$root.noselfroles.forgroup")
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
                return
            }
            val emotejiList = mutableListOf<String>()
            val size = selfRoles.length()
            for (i in 0 until size) {
                if (!range.contains(i)) continue
                val dataEntry = selfRoles.getArray(i)
                val emoteji = dataEntry.getString(0)
                emotejiList.add(emoteji)
            }
            if (appendMessageId) {
                val ids = group.messageIds.toMutableList()
                ids.addIfNotPresent(message.idLong)
                group.messageIds = ids
                group.channelId = channel.idLong
                context.daoManager.selfRoleGroupWrapper.insertOrUpdate(context.guildId, group)
            }

            val externalEmotes = containsExternalEmotes(context, emotejiList)
            if (externalEmotes && notEnoughPermissionsAndMessage(context, channel, Permission.MESSAGE_EXT_EMOJI)) return

            SendGroupAutoArg.addEmotejisToMsg(emotejiList, message, context)
            val msg = "Added **${emotejiList.size}** emotejis to the message in ${channel.asTag}".run {
                if (appendMessageId) {
                    this + "\nAlso added ${message.idLong} to the messageIds in `${group.groupName.escapeCodeblockMarkdown()}`"
                } else this
            }
            sendRsp(context, msg)
        }

        companion object {
            fun containsExternalEmotes(
                context: ICommandContext,
                emotejiList: MutableList<String>
            ) = emotejiList.any {
                if (it.isPositiveNumber()) {
                    val emote = context.guild.getEmoteById(it)
                    emote == null
                } else false
            }
        }
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class SetNameArg(parent: String) : AbstractCommand("$parent.setname") {

        init {
            name = "setName"
            aliases = arrayOf("sn")
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 1)) return

            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

            val pair = getEmotejiByArgsNMessage(context, 1) ?: return
            var emName: String? = null
            val emoteji = if (pair.first == null) {
                pair.second?.let { emName = it }
                pair.second
            } else {
                pair.first?.name?.let { emName = it }
                pair.first?.id
            } ?: return

            val name = emName
            require(name != null) { "what.." }

            val wrapper = context.daoManager.selfRoleWrapper
            val map = wrapper.getMap(context.guildId)

            val data = map[group.groupName] ?: return

            for (i in 0 until data.length()) {
                val dataEntry = data.getArray(i)
                if (dataEntry.getString(0) == emoteji) {
                    if (context.args.size == 2) {
                        var dName = dataEntry.getString(1)
                        if (dName.isBlank()) {
                            dName = "/"
                        }

                        val msg = context.getTranslation("$root.show.")
                            .withVariable("group", group.groupName)
                            .withVariable("emoteji", emoteji)
                            .withVariable("name", dName)

                        sendRsp(context, msg)
                    } else {
                        val dName = getStringFromArgsNMessage(context, 2, 1, 32) ?: return
                        val roleData = dataEntry.getArray(2)
                        val getAllRoles = dataEntry.getBoolean(3)
                        dataEntry.remove(1)
                        dataEntry.remove(1)
                        dataEntry.remove(1)
                        dataEntry.add(dName)
                        dataEntry.add(roleData)
                        dataEntry.add(getAllRoles)

                        data.remove(i)
                        data.add(dataEntry)

                        wrapper.update(context.guildId, group.groupName, data)

                        val msg = context.getTranslation("$root.set")
                            .withVariable("group", group.groupName)
                            .withVariable("emoteji", emoteji)
                            .withVariable("name", dName)

                        sendRsp(context, msg)
                    }

                    break
                }
            }
        }
    }

    class SetGetAllRolesArg(parent: String) : AbstractCommand("$parent.setgetallroles") {

        init {
            name = "setGetAllRoles"
            aliases = arrayOf("sgar")
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 1)) return
            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

            val pair = getEmotejiByArgsNMessage(context, 1) ?: return
            var emName: String? = null
            val emoteji = if (pair.first == null) {
                pair.second?.let { emName = it }
                pair.second
            } else {
                pair.first?.name?.let { emName = it }
                pair.first?.id
            } ?: return

            val name = emName
            require(name != null) { "what.." }

            val wrapper = context.daoManager.selfRoleWrapper
            val map = wrapper.getMap(context.guildId)

            val data = map[group.groupName] ?: return

            for (i in 0 until data.length()) {
                val dataEntry = data.getArray(i)
                if (dataEntry.getString(0) == emoteji) {
                    if (context.args.size == 2) {
                        val state = dataEntry.getBoolean(3)
                        val msg = context.getTranslation("$root.show.$state")
                            .withVariable("group", group.groupName)
                            .withVariable("emoteji", emoteji)

                        sendRsp(context, msg)
                    } else {
                        val state = getBooleanFromArgNMessage(context, 2) ?: return
                        dataEntry.remove(3)
                        dataEntry.add(state)

                        data.remove(i)
                        data.add(dataEntry)

                        wrapper.update(context.guildId, group.groupName, data)

                        val msg = context.getTranslation("$root.set.$state")
                            .withVariable("group", group.groupName)
                            .withVariable("emoteji", emoteji)

                        sendRsp(context, msg)
                    }

                    break
                }
            }
        }
    }

    // Sends the group and sets the channel ect aka the big mess
    class SendGroupAutoArg(parent: String) : AbstractCommand("$parent.sendgroupauto") {

        init {
            name = "sendGroupAuto"
            aliases = arrayOf("sg", "sga")
        }

        companion object {
            suspend fun addEmotejisToMsg(
                emotejisForMsg: List<String>,
                msg: Message,
                context: ICommandContext
            ) {
                for (emoteji in emotejisForMsg) {
                    if (emoteji.isPositiveNumber()) {
                        val emote = context.guild.getEmoteById(emoteji)
                            ?: context.shardManager.getEmoteById(emoteji)
                            ?: context.daoManager.emoteCache.getEmote(emoteji.toLong())?.run {
                                val guildImpl =
                                    context.shardManager.guilds.first { it.idLong != msg.guild.idLong } as GuildImpl
                                EmoteImpl(emoteji.toLong(), guildImpl)
                                    .setName(this.name)
                                    .setAnimated(this.isAnimated)
                                    .setAvailable(this.isAvailable)
                                    .setManaged(this.isManaged)
                            }
                        emote?.let { msg.addReaction(it).queue() }
                    } else {
                        Emojis.ofUnicode(emoteji) ?: continue
                        msg.addReaction(emoteji).queue()
                    }
                }
            }
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 0)) return

            val wrapper = context.daoManager.selfRoleGroupWrapper
            val wrapper2 = context.daoManager.selfRoleWrapper
            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
            val selfRoles = wrapper2.getMap(context.guildId)[group.groupName]

            if (selfRoles == null || selfRoles.isEmpty) {
                val msg = context.getTranslation("$root.noselfroles.forgroup")
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
                return
            }

            val channel = if (context.args.size > 1) {
                getTextChannelByArgsNMessage(context, 1) ?: return
            } else context.textChannel

            if (notEnoughPermissionsAndMessage(
                    context,
                    channel,
                    Permission.MESSAGE_WRITE,
                    Permission.MESSAGE_EMBED_LINKS,
                    Permission.MESSAGE_READ,
                    Permission.MESSAGE_ADD_REACTION,
                    Permission.MESSAGE_HISTORY
                )
            ) return

            val bodyFormat = group.pattern ?: context.getTranslation("$root.bodyformat")

            val embedder = Embedder(context)

            val emotejiList = mutableListOf<String>()
            val entries = mutableListOf<String>()
            val size = selfRoles.length()
            for (i in 0 until size) {
                val dataEntry = selfRoles.getArray(i)
                val emoteji = dataEntry.getString(0)
                emotejiList.add(emoteji)
                val name = dataEntry.getString(1)
                var roleMention = ""
                val roleData = dataEntry.getArray(2)
                for (j in 0 until roleData.length()) {
                    val roleId = roleData.getArray(j).getLong(1)
                    val role = context.guild.getRoleById(roleId)
                    roleMention += (role?.asMention ?: "error") + ", "
                }
                roleMention = roleMention.removeSuffix(", ")

                val emoteValue = when {
                    emoteji.isPositiveNumber() -> getEmote(context, emoteji.toLong())?.asMention
                    else -> emoteji
                } ?: "error"
                val body = bodyFormat
                    .withVariable("name", name)
                    .withVariable("role", roleMention)
                    .withVariable("roleMention", roleMention) // legacy support
                    .withVariable("enter", "\n")
                    .withVariable("emoteji", emoteValue)

                entries.add(body)
            }

            val external = AddReactionsToMessage.containsExternalEmotes(context, emotejiList)
            if (external && notEnoughPermissionsAndMessage(context, channel, Permission.MESSAGE_EXT_EMOJI)) return
            var alreadyUsedEmotesAmount = 0
            val messages = mutableListOf<Long>()
            val totalLength = entries.sumOf { it.length }
            val embedDisabledWrapper = context.daoManager.embedDisabledWrapper

            val titleFormat = context.getTranslation("$root.titleformat.part")
            if (entries.size > 20 || totalLength > MessageEmbed.TEXT_MAX_LENGTH) {
                val body = StringBuilder()
                var part = 1
                for ((index, entry) in entries.withIndex()) {
                    if (body.length + entry.length > MessageEmbed.TEXT_MAX_LENGTH || (index + 1) % 20 == 0) {
                        val title = titleFormat
                            .withVariable("group", group.groupName)
                            .withVariable("part", part++)
                        val embed = embedder.setTitle(title)
                            .setDescription(body)
                            .build()

                        val msg = sendEmbedAwaitEL(embedDisabledWrapper, channel, embed).firstOrNull()
                        if (msg != null) {
                            messages.add(msg.idLong)
                            val emotejisForMsg = emotejiList.subList(alreadyUsedEmotesAmount, index)
                            alreadyUsedEmotesAmount += emotejisForMsg.size
                            addEmotejisToMsg(emotejisForMsg, msg, context)
                        }
                        body.clear()
                    }
                    body.append(entry)

                }
                if (body.isNotBlank()) {
                    val title = titleFormat
                        .withVariable("group", group.groupName)
                        .withVariable("part", part)
                    val embed = embedder.setTitle(title)
                        .setDescription(body)
                        .build()

                    val msg = sendEmbedAwaitEL(embedDisabledWrapper, channel, embed).firstOrNull()
                    if (msg != null) {
                        messages.add(msg.idLong)
                        val emotejisForMsg = emotejiList.drop(alreadyUsedEmotesAmount)
                        addEmotejisToMsg(emotejisForMsg, msg, context)
                    }
                }
            } else {
                val title = context.getTranslation("$root.titleformat")
                    .withVariable("group", group.groupName)
                embedder.setTitle(title)
                for (entry in entries) {
                    embedder.appendDescription(entry)
                }

                val msg = sendEmbedAwaitEL(embedDisabledWrapper, channel, embedder.build()).firstOrNull()
                if (msg != null) {
                    messages.add(msg.idLong)
                    addEmotejisToMsg(emotejiList, msg, context)
                }
            }

            val messageIds = messages.sorted()

            group.messageIds = messageIds
            group.channelId = channel.idLong

            wrapper.insertOrUpdate(context.guildId, group)

            val msg = context.getTranslation("$root.sent")
                .withVariable("group", group.groupName)
                .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
            sendRsp(context, msg)
        }
    }

    class GroupArg(parent: String) : AbstractCommand("$parent.group") {

        init {
            name = "group"
            aliases = arrayOf("g")
            children = arrayOf(   // Groups will have lite ids, the arg for add will be the displayname
                AddArg(root),     // Adds a group
                RemoveArg(root),  // Removes a group
                ListArg(root),    // Lists all roles in group
                MessageIdsArg(root),     // Message Ids
                SetChannel(root),
                SetSelfRoleAbleArg(root),
                SetRequirePermission(root),
                SetEnabledArg(root),
                SetLimitToOneRole(root),
                SetPattern(root),
                ChangeNameArg(root)
            )
        }

        class SetRequirePermission(parent: String) : AbstractCommand("$parent.setrequirepermission") {

            init {
                name = "setRequirePermission"
                aliases = arrayOf("srp")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return

                val wrapper = context.daoManager.selfRoleGroupWrapper
                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size == 1) {
                    val msg = context.getTranslation("$root.show." + group.limitToOneRole)
                        .withVariable("group", group.groupName)
                    sendRsp(context, msg)
                    return
                }

                val bool = getBooleanFromArgNMessage(context, 1) ?: return
                group.limitToOneRole = bool
                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set." + group.limitToOneRole)
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
                return
            }

        }

        class SetLimitToOneRole(parent: String) : AbstractCommand("$parent.setlimittoonerole") {

            init {
                name = "setLimitToOneRole"
                aliases = arrayOf("sltor")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return

                val wrapper = context.daoManager.selfRoleGroupWrapper
                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size == 1) {
                    val msg = context.getTranslation("$root.show." + group.limitToOneRole)
                        .withVariable("group", group.groupName)
                    sendRsp(context, msg)
                    return
                }

                val bool = getBooleanFromArgNMessage(context, 1) ?: return
                group.limitToOneRole = bool
                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set." + group.limitToOneRole)
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
                return
            }
        }

        class SetPattern(parent: String) : AbstractCommand("$parent.setpattern") {

            init {
                name = "setPattern"
                aliases = arrayOf("sp")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return

                val wrapper = context.daoManager.selfRoleGroupWrapper
                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

                if (context.args.size == 1) {
                    val unset = if (group.pattern == null) ".unset" else ""

                    val msg = context.getTranslation("$root.show$unset")
                        .withVariable("group", group.groupName)
                        .withVariable("pattern", group.pattern ?: "null")

                    sendRsp(context, msg)
                    return
                }

                if (context.args[1] == "null") {
                    group.pattern = null
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.unset")
                        .withVariable("group", group.groupName)

                    sendRsp(context, msg)
                    return
                } else {
                    val newPattern = getStringFromArgsNMessage(context, 1, 1, 256) ?: return
                    group.pattern = newPattern

                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.set")
                        .withVariable("group", group.groupName)
                        .withVariable("pattern", newPattern)

                    sendRsp(context, msg)
                    return
                }
            }
        }

        class SetChannel(parent: String) : AbstractCommand("$parent.setchannel") {

            init {
                name = "setChannel"
                aliases = arrayOf("sc")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return

                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper

                if (context.args.size == 1) {
                    val textChannel = context.guild.getTextChannelById(group.channelId)
                    val msg = if (textChannel == null) {
                        context.getTranslation("$root.show.unset")
                    } else {
                        context.getTranslation("$root.show")
                            .withVariable("group", group.groupName)
                            .withVariable("channel", textChannel.asTag)
                    }
                    sendRsp(context, msg)
                    return
                }

                if (context.args[1].equals("null", true)) {
                    group.channelId = -1
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.unset")
                        .withVariable("group", group.groupName)
                    sendRsp(context, msg)
                    return
                }

                val channel = getTextChannelByArgsN(context, 1) ?: return

                group.channelId = channel.idLong
                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set")
                    .withVariable("group", group.groupName)
                    .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                sendRsp(context, msg)
                return
            }
        }

        class MessageIdsArg(parent: String) : AbstractCommand("$parent.messageids") {

            init {
                name = "messageIds"
                aliases = arrayOf("mid", "mids")
                children = arrayOf(
                    AddArg(root),
                    RemoveArg(root),
                    RemoveAtArg(root),
                    ClearArg(root),
                    ListArg(root)
                )
            }

            class AddArg(parent: String) : AbstractCommand("$parent.add") {

                init {
                    name = "add"
                    aliases = arrayOf("a")
                }

                override suspend fun execute(context: ICommandContext) {
                    if (argSizeCheckFailed(context, 1)) return

                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                    val argument = getLongFromArgNMessage(context, 1, 1000000000000000) ?: return
                    val wrapper = context.daoManager.selfRoleGroupWrapper

                    group.messageIds = group.messageIds + argument
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.added")
                        .withVariable("group", group.groupName)
                        .withVariable("messageId", "$argument")

                    sendRsp(context, msg)
                }
            }

            class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                init {
                    name = "remove"
                    aliases = arrayOf("rm", "r")
                }

                override suspend fun execute(context: ICommandContext) {
                    if (argSizeCheckFailed(context, 1)) return

                    val wrapper = context.daoManager.selfRoleGroupWrapper
                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                    val messageId = getLongFromArgNMessage(context, 1, 1000000000000000) ?: return

                    group.messageIds = group.messageIds - messageId
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.removed")
                        .withVariable("group", group.groupName)
                        .withVariable("messageId", "$messageId")

                    sendRsp(context, msg)
                }
            }

            class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

                init {
                    name = "removeAt"
                    aliases = arrayOf("rma", "ra")
                }

                override suspend fun execute(context: ICommandContext) {
                    if (argSizeCheckFailed(context, 1)) return

                    val wrapper = context.daoManager.selfRoleGroupWrapper
                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                    val inputIndex = getIntegerFromArgNMessage(context, 1, 1, group.messageIds.size)
                        ?: return
                    val index = inputIndex - 1

                    val messages = group.messageIds.sorted().toMutableList()
                    val messageId = messages[index]
                    messages.removeAt(index)

                    group.messageIds = messages
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.removed")
                        .withVariable("group", group.groupName)
                        .withVariable("index", "$inputIndex")
                        .withVariable("messageId", "$messageId")

                    sendRsp(context, msg)
                }
            }

            class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("cls", "c")
                }

                override suspend fun execute(context: ICommandContext) {
                    if (argSizeCheckFailed(context, 0)) return

                    val wrapper = context.daoManager.selfRoleGroupWrapper
                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

                    group.messageIds = emptyList()
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.cleared")
                        .withVariable("group", group.groupName)

                    sendRsp(context, msg)
                }
            }

            class ListArg(parent: String) : AbstractCommand("$parent.list") {

                init {
                    name = "list"
                    aliases = arrayOf("ls", "l")
                }

                override suspend fun execute(context: ICommandContext) {
                    if (argSizeCheckFailed(context, 0)) return

                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

                    if (group.messageIds.isEmpty()) {
                        val msg = context.getTranslation("$root.empty")
                            .withVariable("group", group.groupName)
                        sendRsp(context, msg)
                        return
                    }

                    val title = context.getTranslation("$root.title")
                        .withVariable("group", group.groupName)
                    val content = StringBuilder("```INI\n[index] - [messageId]")

                    for ((index, id) in group.messageIds.sorted().withIndex()) {
                        content.append("\n${index + 1} - [$id]")
                    }

                    content.append("```")

                    val msg = title + content.toString()
                    sendRsp(context, msg)
                }
            }

            override suspend fun execute(context: ICommandContext) {
                sendSyntax(context)
            }
        }

        class SetSelfRoleAbleArg(parent: String) : AbstractCommand("$parent.setselfroleable") {

            init {
                name = "setSelfRoleable"
                aliases = arrayOf("ssr")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return
                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size < 2) {
                    val msg = context.getTranslation("$root.show.${group.isEnabled}")
                        .withVariable("group", group.groupName)
                    sendRsp(context, msg)
                    return
                }

                val state = getBooleanFromArgNMessage(context, 1) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper
                group.isSelfRoleable = state

                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set.$state")
                    .withVariable("group", group.groupName)

                sendRsp(context, msg)
            }
        }

        class SetEnabledArg(parent: String) : AbstractCommand("$parent.setenabled") {

            init {
                name = "setEnabled"
                aliases = arrayOf("se")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return

                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size < 2) {
                    val msg = context.getTranslation("$root.show.${group.isEnabled}")
                        .withVariable("group", group.groupName)
                    sendRsp(context, msg)
                    return
                }

                val state = getBooleanFromArgNMessage(context, 1) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper
                group.isEnabled = state

                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set.$state")
                    .withVariable("group", group.groupName)

                sendRsp(context, msg)
            }
        }

        class AddArg(val parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return

                val name = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper

                val sr = getSelfRoleGroupByGroupNameN(context, name)
                if (sr != null) {
                    val msg = context.getTranslation("$parent.exists")
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                        .withVariable(PLACEHOLDER_ARG, name)
                    sendRsp(context, msg)
                    return
                }

                val selfRoleAble = if (context.args.size > 1) {
                    getBooleanFromArgNMessage(context, 1) ?: return
                } else true
                val limitToOneRole = if (context.args.size > 2) {
                    getBooleanFromArgNMessage(context, 2) ?: return
                } else false
                val requiresPermission = if (context.args.size > 2) {
                    getBooleanFromArgNMessage(context, 3) ?: return
                } else false

                val newSr = SelfRoleGroup(
                    name,
                    emptyList(),
                    channelId = -1,
                    isEnabled = true,
                    isSelfRoleable = selfRoleAble,
                    pattern = null,
                    limitToOneRole = limitToOneRole,
                    requiresPermission = requiresPermission
                )
                wrapper.insertOrUpdate(context.guildId, newSr)

                val msg = context.getTranslation("$root.added")
                    .withVariable("name", name)
                sendRsp(context, msg)
            }
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("r", "d", "rm", "delete")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 0)) return

                val selfRoleGroup = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper
                wrapper.delete(context.guildId, selfRoleGroup.groupName)

                val msg = context.getTranslation("$root.removed")
                    .withVariable("name", selfRoleGroup.groupName)
                sendRsp(context, msg)
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("l", "ls")
            }

            override suspend fun execute(context: ICommandContext) {
                val list = context.daoManager.selfRoleGroupWrapper.getMap(context.guildId)
                    .sortedBy { it.groupName }

                val title = context.getTranslation("$root.title")
                val content =
                    StringBuilder("```INI\n[index] - [name] - ([channelId], [channelName]) - [selfAssignable] - [enabled]")

                for ((index, srg) in list.withIndex()) {
                    val channel = context.guild.getTextChannelById(srg.channelId)
                    content.append("\n${index + 1} - ").append(srg.groupName)
                        .append(" - (").append(channel?.idLong ?: -1).append(", ").append(channel?.name ?: "null")
                        .append(") - ").append(srg.isSelfRoleable).append(" - ").append(srg.isEnabled)
                }
                content.append("```")
                val msg = title + content.toString()
                sendRsp(context, msg)
            }
        }

        class ChangeNameArg(val parent: String) : AbstractCommand("$parent.changename") {

            init {
                name = "changeName"
                aliases = arrayOf("cn")
            }

            override suspend fun execute(context: ICommandContext) {
                if (argSizeCheckFailed(context, 1)) return

                val selfRoleGroup1 = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                val name = getStringFromArgsNMessage(context, 1, 1, 64) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper

                val selfRoleGroup2 = getSelfRoleGroupByGroupNameN(context, name)
                if (selfRoleGroup2 != null) {
                    val msg = context.getTranslation("$parent.exists")
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                        .withVariable(PLACEHOLDER_ARG, name)
                    sendRsp(context, msg)
                    return
                }

                context.daoManager.selfRoleWrapper.changeName(context.guildId, selfRoleGroup1.groupName, name)

                selfRoleGroup1.apply {
                    val newSr = SelfRoleGroup(
                        name,
                        messageIds,
                        channelId,
                        isEnabled,
                        pattern,
                        isSelfRoleable,
                        limitToOneRole,
                        requiresPermission
                    )
                    wrapper.delete(context.guildId, groupName)
                    wrapper.insertOrUpdate(context.guildId, newSr)

                    val msg = context.getTranslation("$root.updated")
                        .withVariable("oldName", groupName)
                        .withVariable("newName", newSr.groupName)
                    sendRsp(context, msg)
                }
            }
        }

        override suspend fun execute(context: ICommandContext) {
            sendSyntax(context)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("set", "a", "put")
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 2)) return

            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

            if (context.args.size == 3 || context.args.size == 4) {
                val pair = getEmotejiByArgsNMessage(context, 1) ?: return
                var rname: String? = null
                val id = if (pair.first == null) {
                    pair.second?.let { rname = it }
                    pair.second
                } else {
                    pair.first?.name?.let { rname = it }
                    pair.first?.id.toString()
                } ?: return

                val name = rname
                require(name != null) { "what.." }

                val role = getRoleByArgsNMessage(context, 2) ?: return
                var chance = 100
                val msg = if (context.args.size > 3) {
                    chance = getIntegerFromArgNMessage(context, 3, 1) ?: return
                    context.getTranslation("$root.success.chance")
                        .withVariable("chance", "$chance")
                } else {
                    context.getTranslation("$root.success")
                }.withVariable("group", group.groupName)
                    .withVariable("emoteji", name)
                    .withVariable(PLACEHOLDER_ROLE, role.name)

                context.daoManager.selfRoleWrapper.set(context.guildId, group.groupName, id, role.idLong, chance)
                sendRsp(context, msg)
            } else {
                val manyMap = context.fullArg.splitIETEL("\n").map { it.split(SPACE_PATTERN) }
                var count = 1
                for (entry in manyMap) {
                    val entrySize = entry.size
                    if (entrySize < 2) {
                        sendSyntax(context)
                        return
                    }

                    val pair = getEmotejiByArgsNMessage(context, count++) ?: return
                    var rname: String? = null
                    val id = if (pair.first == null) {
                        pair.second?.let { rname = it }
                        pair.second
                    } else {
                        pair.first?.name?.let { rname = it }
                        pair.first?.id.toString()
                    } ?: return

                    val name = rname
                    require(name != null) { "what.." }

                    val role = getRoleByArgsNMessage(context, count++) ?: return
                    var chance = 100
                    if (entry.size > 2 || (count == 3 && entry.size > 3)) {
                        chance = getIntegerFromArgNMessage(context, 3, count++) ?: return
                    }

                    context.daoManager.selfRoleWrapper.set(context.guildId, group.groupName, id, role.idLong, chance)
                }
                sendRsp(context, "Added selfroles in bulk")
            }
        }
    }

    class ClearArg(val parent: String) : AbstractCommand("$parent.clear") {

        init {
            name = "clear"
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 0)) return

            val groupName = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
            val selfRoleWrapper = context.daoManager.selfRoleWrapper
            val rows = selfRoleWrapper.clear(context.guildId, groupName)

            val msg = context.getTranslation("$root.cleared")
                .withVariable("rows", rows)
                .withSafeVariable("group", groupName)
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "rm", "delete", "del", "d")
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 1)) return

            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
            val pair = getEmotejiByArgsN(context, 1) ?: return
            val selfRoleWrapper = context.daoManager.selfRoleWrapper
            val guildSelfRoles = selfRoleWrapper.getMap(context.guildId)[group.groupName]
            if (guildSelfRoles == null) {
                sendRsp(context, "There is no selfrole entry in the " + group.groupName + " for that emoteji")
                return
            }

            val emotejis = mutableListOf<String>()
            val roleIds = mutableListOf<Long>()
            for (i in 0 until guildSelfRoles.length()) {
                val entryData = guildSelfRoles.getArray(i)
                emotejis.add(entryData.getString(0))
                val roleDataArr = entryData.getArray(2)
                for (j in 0 until roleDataArr.length()) {
                    roleIds.add(roleDataArr.getArray(j).getLong(1))
                }
            }

            val emoteji = if (pair.first == null) {
                pair.second
            } else {
                pair.first?.id.toString()
            } ?: return

            val emoteName = if (pair.first == null) {
                pair.second
            } else {
                pair.first?.asMention
            } ?: return

            if (!emotejis.contains(emoteji)) {
                val msg = context.getTranslation("$root.emotejivoid")
                    .withVariable("emoteji", emoteName)
                    .withVariable("group", group.groupName)
                sendRsp(context, msg)
                return
            }

            val roles: String

            if (context.args.size > 2) {
                val roleId = if (context.args[2].isPositiveNumber()) {
                    roles = "<@&" + context.args[2] + ">"
                    val roleId = context.args[2].toLongOrNull()
                    if (roleId == null) {
                        sendRsp(context, "Number too big")
                        return
                    }
                    roleId
                } else {
                    val role = getRoleByArgsNMessage(context, 2) ?: return
                    roles = role.asMention
                    role.idLong
                }
                selfRoleWrapper.remove(context.guildId, group.groupName, emoteji, roleId)
            } else {
                roles = roleIds.joinToString(", ") { "<@&$it>" }
                selfRoleWrapper.remove(context.guildId, group.groupName, emoteji)
            }

            val msg = context.getTranslation("$root.success")
                .withVariable("group", group.groupName)
                .withVariable("emoteName", emoteName)
                .withVariable("role", roles)

            sendRsp(context, msg)
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "ra")
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 1)) return

            val selfRoleWrapper = context.daoManager.selfRoleWrapper
            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
            val guildSelfRoles = selfRoleWrapper.getMap(context.guildId)[group.groupName]
            if (guildSelfRoles == null) {
                sendRsp(context, "There is not selfrole entry in the " + group.groupName + " for that emoteji")
                return
            }
            val index = getIntegerFromArgNMessage(context, 1, 1, guildSelfRoles.length()) ?: return

            val dataEntry = guildSelfRoles.getArray(index - 1)
            val emoteji = dataEntry.getString(0)

            val rolesIds = mutableListOf<Long>()
            val roleDataArr = dataEntry.getArray(2)
            for (j in 0 until roleDataArr.length()) {
                dataEntry.add(roleDataArr.getArray(j).getLong(1))
            }

            val roleString = rolesIds.joinToString(", ") { "<@&$it>" }
            selfRoleWrapper.remove(context.guildId, group.groupName, emoteji)

            val msg = context.getTranslation("$root.success")
                .withVariable("group", group.groupName)
                .withVariable("index", index)
                .withVariable("emoteji", emoteji)
                .withVariable("role", roleString)

            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.selfRoleWrapper
            val map = wrapper.getMap(context.guildId)

            val msg = if (map.isNotEmpty()) {
                val title = context.getTranslation("$root.title")
                val content =
                    StringBuilder("```ini\n[group]:\n [index] - [emoteji] - [name] -> [(chance, roleId, roleName), ...] - [getAll]")

                for ((group, dataArray) in map.toSortedMap()) {
                    content.append("\n${group}:")
                    var counter = 1

                    for (i in 0 until dataArray.length()) {
                        val dataEntry = dataArray.getArray(i)
                        val emoteji = dataEntry.getString(0)
                        val name = dataEntry.getString(1)
                        val getAll = dataEntry.getBoolean(3)

                        val roleDataArr = dataEntry.getArray(2)

                        var roleIdString = ""
                        for (j in 0 until roleDataArr.length()) {
                            val roleInfoArr = roleDataArr.getArray(j)
                            val roleId = roleInfoArr.getLong(1)
                            val roleChance = roleInfoArr.getLong(0)
                            val role = context.guild.getRoleById(roleId)
                            roleIdString += "(" + roleChance + ", " + (role?.idLong ?: "error") + ", " + (role?.name
                                ?: "error") + ")" + ", "
                        }

                        roleIdString = roleIdString.removeSuffix(", ")

                        content.append("\n ${counter++} - [$emoteji] - [$name] -> [$roleIdString] - $getAll")
                    }
                }
                content.append("```")
                title + content
            } else {
                context.getTranslation("$root.empty")
            }
            sendRspCodeBlock(context, msg, "INI")
        }
    }
}

suspend fun getSelfRoleGroupByGroupNameN(context: ICommandContext, groupName: String): SelfRoleGroup? {
    val wrapper = context.daoManager.selfRoleGroupWrapper
    return wrapper.getMap(context.guildId).firstOrNull { group ->
        group.groupName == groupName
    }
}

suspend fun getSelfRoleGroupByArgNMessage(context: ICommandContext, index: Int): SelfRoleGroup? {
    val wrapper = context.daoManager.selfRoleGroupWrapper
    val group = getStringFromArgsNMessage(context, index, 1, 64)
        ?: return null
    val selfRoleGroup = wrapper.getMap(context.guildId).firstOrNull { (groupName) ->
        groupName == group
    }
    if (selfRoleGroup == null) {
        val msg = context.getTranslation("message.unknown.selfrolegroup")
            .withVariable(PLACEHOLDER_ARG, group)
        sendRsp(context, msg)
    }
    return selfRoleGroup
}