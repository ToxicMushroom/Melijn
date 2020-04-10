package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.role.SelfRoleGroup
import me.melijn.melijnbot.database.role.SelfRoleMode
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import kotlin.math.max

const val UNKNOWN_SELFROLEMODE_PATH = "message.unknown.selfrolemode"

class SelfRoleCommand : AbstractCommand("command.selfrole") {

    init {
        id = 37
        name = "selfRole"
        aliases = arrayOf("sr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            SendGroupAuto(root),
            ListArg(root),
            GroupArg(root),
            SetMode(root),  // Manual, Auto | Auto will ignore selfRoleMessageIds and selfRoleChannelIds and use the internal cached ones
            SendGroup(root) // Internal cached ones are messageIds created by the >sr sendGroup <channel> command
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    // Sends the group
    class SendGroup(parent: String) : AbstractCommand("$parent.sendgroup") {

        init {
            name = "sendGroup"
            aliases = arrayOf("sg")
        }

        override suspend fun execute(context: CommandContext) {
            sendMsg(context, "WIP")
//            if (context.args.isEmpty()) {
//                sendSyntax(context)
//                return
//            }
//
//            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
//
//            val channel = if (context.args.size < 2) {
//                context.textChannel
//
//            } else {
//                getTextChannelByArgsNMessage(context, 1)
//            } ?: return
//
//            val messages: List<Message> = sendMsg(channel, "test ${group.groupName}")


        }
    }

    // Sends the group and sets the channel ect aka the big mess
    class SendGroupAuto(parent: String) : AbstractCommand("$parent.sendgroupauto") {

        init {
            name = "sendGroupAuto"
            aliases = arrayOf("sg")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.selfRoleGroupWrapper
            val wrapper2 = context.daoManager.selfRoleWrapper
            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
            val selfRoles = wrapper2.selfRoleCache[context.guildId].await()[group.groupName]

            if (selfRoles == null || selfRoles.isEmpty()) {
                val msg = context.getTranslation("$root.noselfroles.forgroup")
                    .replace("%group%", group.groupName)
                sendMsg(context, msg)
                return
            }


            val channel = if (context.args.size < 2) {
                context.textChannel

            } else {
                getTextChannelByArgsNMessage(context, 1)
            } ?: return


            val bodyFormat = context.getTranslation("$root.bodyformat")

            val embedder = Embedder(context)


            var body = ""
            val size = selfRoles.size
            for ((emoteji, roleIds) in selfRoles.toSortedMap()) {
                var roleMention = ""
                for (roleId in roleIds) {
                    val role = context.guild.getRoleById(roleId)
                    roleMention += role?.asMention ?: "error" + ", "
                }
                roleMention = roleMention.removeSuffix(", ")

                val isEmoji = SupportedDiscordEmoji.helpMe.contains(emoteji)

                body += if (isEmoji) {
                    bodyFormat
                        .replace("%role%", roleMention)
                        .replace("%emoteji%", emoteji)
                } else {
                    val emote = context.guild.getEmoteById(emoteji) ?: context.shardManager.getEmoteById(emoteji)
                    bodyFormat
                        .replace("%role%", roleMention)
                        .replace("%emoteji%", emote?.asMention ?: "error")
                }
            }

            val totalCount = body.count { c -> c == '\n' }
            val shouldCount = selfRoles.size
            val ratio = totalCount / shouldCount

            var alreadyEmotesAmount = 0
            val messages: List<Message> = if (size > 20 || body.length > MessageEmbed.TEXT_MAX_LENGTH) {
                val splitted = StringUtils.splitMessageAtMaxCharAmountOrLength(body, 20, '\n', MessageEmbed.TEXT_MAX_LENGTH)

                val messages = mutableListOf<Message>()
                val titleFormat = context.getTranslation("$root.titleformat.part")
                for ((index, part) in splitted.withIndex()) {

                    embedder.setTitle(titleFormat
                        .replace("%group%", group.groupName)
                        .replace("%part%", index + 1)
                    )

                    embedder.setDescription(part)
                    val messagesPart = sendEmbed(context.daoManager.embedDisabledWrapper, channel, embedder.build())

                    val emoteAmount = part.count { c -> c == '\n' } / max(ratio, 1)
                    val emoteMessage = messagesPart.last()
                    for (emoteIndex in alreadyEmotesAmount until (alreadyEmotesAmount + emoteAmount)) {
                        val emoteji = selfRoles.keys.sorted()[emoteIndex]
                        val isEmoji = SupportedDiscordEmoji.helpMe.contains(emoteji)
                        if (isEmoji) {
                            emoteMessage.addReaction(emoteji).queue()
                        } else {
                            val emote = context.guild.getEmoteById(emoteji)
                                ?: context.shardManager.getEmoteById(emoteji)
                            emote?.let { emoteMessage.addReaction(it).queue() }
                        }
                    }


                    alreadyEmotesAmount += emoteAmount
                    messages.addAll(messagesPart)
                }
                messages.toList()
            } else {
                val titleFormat = context.getTranslation("$root.titleformat")
                embedder.setTitle(titleFormat.replace("%group%", group.groupName))
                embedder.setDescription(body)
                val messagesPart = sendEmbed(context.daoManager.embedDisabledWrapper, channel, embedder.build())

                val emoteMessage = messagesPart.last()
                for (emoteIndex in alreadyEmotesAmount until (alreadyEmotesAmount + shouldCount)) {
                    val emoteji = selfRoles.keys.sorted()[emoteIndex]
                    val isEmoji = SupportedDiscordEmoji.helpMe.contains(emoteji)
                    if (isEmoji) {
                        emoteMessage.addReaction(emoteji).queue()
                    } else {
                        val emote = context.guild.getEmoteById(emoteji)
                            ?: context.shardManager.getEmoteById(emoteji)
                        emote?.let { emoteMessage.addReaction(it).queue() }
                    }
                }


                alreadyEmotesAmount += shouldCount
                messagesPart
            }


            val messageIds = messages.map { it.idLong }.sorted()

            group.messageIds = messageIds
            group.channelId = channel.idLong

            wrapper.insertOrUpdate(context.guildId, group)

            val msg = context.getTranslation("$root.sent")
                .replace("%group%", group.groupName)
                .replace(PLACEHOLDER_CHANNEL, channel.asTag)
            sendMsg(context, msg)
        }
    }

    class SetMode(parent: String) : AbstractCommand("$parent.setmode") {

        init {
            name = "setMode"
            aliases = arrayOf("sm")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.selfRoleModeWrapper
            if (context.args.isEmpty()) {
                val mode = wrapper.selfRoleModeCache.get(context.guildId).await().toUCC()
                val msg = context.getTranslation("$root.show")
                    .replace("%mode%", mode)
                sendMsg(context, msg)
                return
            }

            val mode = getEnumFromArgNMessage<SelfRoleMode>(context, 0, UNKNOWN_SELFROLEMODE_PATH) ?: return
            wrapper.set(context.guildId, mode)
            val msg = context.getTranslation("$root.set")
                .replace("%mode%", mode.toUCC())
            sendMsg(context, msg)
        }
    }

    class GroupArg(parent: String) : AbstractCommand("$parent.group") {

        init {
            name = "group"
            children = arrayOf(   // Groups will have lite ids, the arg for add will be the displayname
                AddArg(root),     // Adds a group
                RemoveArg(root),  // Removes a group
                ListArg(root),    // Lists all roles in group
                MessageIdsArg(root),     // Message Ids
                SetChannel(root), // pepega
                SetSelfRoleAbleArg(root),
                SetEnabledArg(root),
                ChangeNameArg(root)
            )
        }

        class SetChannel(parent: String) : AbstractCommand("$parent.setchannel") {

            init {
                name = "setChannelId"
                aliases = arrayOf("sci")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.isEmpty()) {
                    sendSyntax(context)
                    return
                }

                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper

                if (context.args.size == 1) {
                    val msg = context.getTranslation("$root.show")
                        .replace("%group%", group.groupName)
                        .replace("%channel%", "<#${group.channelId}>")
                    sendMsg(context, msg)
                    return
                }

                if (context.args[1].equals("null", true)) {
                    group.channelId = -1
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.unset")
                        .replace("%group%", group.groupName)
                    sendMsg(context, msg)
                    return
                }

                val channel = getTextChannelByArgsN(context, 1) ?: return

                group.channelId = channel.idLong
                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set")
                    .replace("%group%", group.groupName)
                    .replace(PLACEHOLDER_CHANNEL, channel.asTag)
                sendMsg(context, msg)
                return
            }
        }

        class MessageIdsArg(parent: String) : AbstractCommand("$parent.ids") {

            init {
                name = "ids"
                aliases = arrayOf("i", "id")
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

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                    val argument = getLongFromArgNMessage(context, 1, 1000000000000000) ?: return
                    val wrapper = context.daoManager.selfRoleGroupWrapper

                    group.messageIds = group.messageIds + argument
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.added")
                        .replace("%group%", group.groupName)
                        .replace("%messageId%", "$argument")

                    sendMsg(context, msg)
                }
            }

            class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

                init {
                    name = "remove"
                    aliases = arrayOf("rm", "r")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val wrapper = context.daoManager.selfRoleGroupWrapper
                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                    val messageId = getLongFromArgNMessage(context, 1, 1000000000000000) ?: return

                    group.messageIds = group.messageIds + messageId
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.removed")
                        .replace("%group%", group.groupName)
                        .replace("%messageId%", "$messageId")

                    sendMsg(context, msg)
                }
            }

            class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

                init {
                    name = "removeAt"
                    aliases = arrayOf("rma", "ra")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val wrapper = context.daoManager.selfRoleGroupWrapper
                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                    val index = getIntegerFromArgNMessage(context, 1, 1, group.messageIds.size)
                        ?: return

                    val messages = group.messageIds.toMutableList()
                    val messageId = messages[index]
                    messages.removeAt(index)

                    group.messageIds = messages
                    wrapper.insertOrUpdate(context.guildId, group)

                    val msg = context.getTranslation("$root.removed")
                        .replace("%group%", group.groupName)
                        .replace("%index%", "$index")
                        .replace("%messageId%", "$messageId")

                    sendMsg(context, msg)
                }
            }

            class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

                init {
                    name = "clear"
                    aliases = arrayOf("cls", "c")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val wrapper = context.daoManager.selfRoleGroupWrapper
                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

                    group.messageIds = emptyList()
                    wrapper.insertOrUpdate(context.guildId, group)


                    val msg = context.getTranslation("$root.cleared")
                        .replace("%group%", group.groupName)

                    sendMsg(context, msg)
                }
            }

            class ListArg(parent: String) : AbstractCommand("$parent.list") {

                init {
                    name = "list"
                    aliases = arrayOf("ls", "l")
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.isEmpty()) {
                        sendSyntax(context)
                        return
                    }

                    val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

                    if (group.messageIds.isEmpty()) {
                        val msg = context.getTranslation("$context.empty")
                            .replace("%group%", group.groupName)
                        sendMsg(context, msg)
                        return
                    }

                    val title = context.getTranslation("$root.title")
                        .replace("%group%", group.groupName)
                    val content = StringBuilder("```INI\n[index] - [messageId]")

                    for ((index, id) in group.messageIds.sorted().withIndex()) {
                        content.append("$index - [$id]")
                    }

                    content.append("```")

                    val msg = title + content.toString()
                    sendMsg(context, msg)
                }
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }
        }

        class SetSelfRoleAbleArg(parent: String) : AbstractCommand("$parent.setselfroleable") {

            init {
                name = "setSelfRoleable"
                aliases = arrayOf("ssr")
            }

            override suspend fun execute(context: CommandContext) {
                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size < 2) {
                    val msg = context.getTranslation("$root.show.${group.isEnabled}")
                        .replace("%group%", group.groupName)
                    sendMsg(context, msg)
                    return
                }

                val state = getBooleanFromArgNMessage(context, 1) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper
                group.isSelfRoleable = state

                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set.$state")
                    .replace("%group%", group.groupName)

                sendMsg(context, msg)
            }
        }

        class SetEnabledArg(parent: String) : AbstractCommand("$parent.setenabled") {

            init {
                name = "setEnabled"
                aliases = arrayOf("se")
            }

            override suspend fun execute(context: CommandContext) {
                val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                if (context.args.size < 2) {
                    val msg = context.getTranslation("$root.show.${group.isEnabled}")
                        .replace("%group%", group.groupName)
                    sendMsg(context, msg)
                    return
                }

                val state = getBooleanFromArgNMessage(context, 1) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper
                group.isEnabled = state

                wrapper.insertOrUpdate(context.guildId, group)

                val msg = context.getTranslation("$root.set.$state")
                    .replace("%group%", group.groupName)

                sendMsg(context, msg)
            }
        }

        class AddArg(val parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: CommandContext) {
                val name = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper

                val sr = getSelfRoleGroupByGroupNameN(context, name)
                if (sr != null) {
                    val msg = context.getTranslation("$parent.exists")
                        .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }

                val newSr = SelfRoleGroup(name, emptyList(), -1, isEnabled = true, isSelfRoleable = true)
                wrapper.insertOrUpdate(context.guildId, newSr)

                val msg = context.getTranslation("$root.added")
                    .replace("%name%", name)
                sendMsg(context, msg)
            }
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("r", "d", "rm", "delete")
            }

            override suspend fun execute(context: CommandContext) {
                val selfRoleGroup = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper
                wrapper.delete(context.guildId, selfRoleGroup.groupName)

                val msg = context.getTranslation("$root.removed")
                    .replace("%name%", name)
                sendMsg(context, msg)
            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("l", "ls")
            }

            override suspend fun execute(context: CommandContext) {
                val list = context.daoManager.selfRoleGroupWrapper.selfRoleGroupCache.get(context.guildId)
                    .await()
                    .sortedBy { it.groupName }

                val title = context.getTranslation("$root.title")
                val content = StringBuilder("```INI\n[index] - [name] - [selfAssignable] - [enabled]")

                for ((index, srg) in list.withIndex()) {
                    content.append("\n$index - ").append(srg.groupName).append(" - ").append(srg.isSelfRoleable).append(" - ").append(srg.isEnabled)
                }
                content.append("```")
                val msg = title + content.toString()
                sendMsg(context, msg)
            }
        }

        class ChangeNameArg(val parent: String) : AbstractCommand("$parent.changename") {

            init {
                name = "changeName"
                aliases = arrayOf("cn")
            }

            override suspend fun execute(context: CommandContext) {
                val selfRoleGroup1 = getSelfRoleGroupByArgNMessage(context, 0) ?: return
                val name = getStringFromArgsNMessage(context, 0, 1, 64) ?: return
                val wrapper = context.daoManager.selfRoleGroupWrapper

                val selfRoleGroup2 = getSelfRoleGroupByGroupNameN(context, name)
                if (selfRoleGroup2 != null) {
                    val msg = context.getTranslation("$parent.exists")
                        .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }

                selfRoleGroup1.apply {
                    val newSr = SelfRoleGroup(name, messageIds, channelId, isEnabled, isSelfRoleable)
                    wrapper.delete(context.guildId, groupName)
                    wrapper.insertOrUpdate(context.guildId, newSr)

                    val msg = context.getTranslation("$root.updated")
                        .replace("%oldName%", groupName)
                        .replace("%newName%", newSr.groupName)
                    sendMsg(context, msg)
                }
            }
        }


        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }


    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("set", "a", "put")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 3) {
                sendSyntax(context)
                return
            }

            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return

            val pair = getEmotejiByArgsNMessage(context, 1) ?: return
            var rname: String? = null
            val id = if (pair.first == null) {
                pair.second?.let { rname = it }
                pair.second
            } else {
                pair.first?.name?.let { rname = it }
                pair.first?.id
            } ?: return

            val name = rname
            require(name != null) { "what.." }

            val role = getRoleByArgsNMessage(context, 2) ?: return

            context.daoManager.selfRoleWrapper.add(context.guildId, group.groupName, id, role.idLong)

            val msg = context.getTranslation("$root.success")
                .replace("%group%", group.groupName)
                .replace("%emoteji%", name)
                .replace(PLACEHOLDER_ROLE, role.name)

            sendMsg(context, msg)
        }
    }


    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("r", "rm", "delete", "del", "d")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
            val pair = getEmotejiByArgsN(context, 1) ?: return
            val selfRoleWrapper = context.daoManager.selfRoleWrapper
            val guildSelfRoles = selfRoleWrapper.selfRoleCache.get(context.guildId)
                .await()[group.groupName] ?: throw IllegalArgumentException("Angry boy :c")

            val emoteji = if (pair.first == null) {
                pair.second
            } else {
                pair.first?.id
            } ?: return

            val emoteName = if (pair.first == null) {
                pair.second
            } else {
                pair.first?.asMention
            } ?: return

            if (!guildSelfRoles.containsKey(emoteji)) {
                val msg = context.getTranslation("$root.emotejivoid")
                    .replace("%emoteji%", emoteName)
                sendMsg(context, msg)
                return
            }

            val roles: String

            if (context.args.size > 2) {
                val roleId = getLongFromArgNMessage(context, 2) ?: return
                roles = "<@&$roleId>"
                selfRoleWrapper.remove(context.guildId, group.groupName, emoteji, roleId)
            } else {
                roles = guildSelfRoles.getOrDefault(emoteji, emptyList()).joinToString(", ", "<@&", ">")
                selfRoleWrapper.remove(context.guildId, group.groupName, emoteji)
            }


            val msg = context.getTranslation("$root.success")
                .replace("%group%", group.groupName)
                .replace("%emoteName%", emoteName)
                .replace("%role%", roles)

            sendMsg(context, msg)
        }
    }


    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma", "ra")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val selfRoleWrapper = context.daoManager.selfRoleWrapper
            val group = getSelfRoleGroupByArgNMessage(context, 0) ?: return
            val guildSelfRoles = selfRoleWrapper.selfRoleCache.get(context.guildId)
                .await()[group.groupName] ?: throw IllegalArgumentException("Angry boy :c")
            val index = getIntegerFromArgNMessage(context, 1, 1, guildSelfRoles.size) ?: return

            val emoteji = guildSelfRoles.keys.sorted()[index]
            val rolesIds = guildSelfRoles[emoteji] ?: throw IllegalArgumentException("angry bwoy")

            val roleString = rolesIds.joinToString(", ", "<@&", ">")
            selfRoleWrapper.remove(context.guildId, group.groupName, emoteji)


            val msg = context.getTranslation("$root.success")
                .replace("%group%", group.groupName)
                .replace("%index%", index)
                .replace("%emoteji%", emoteji)
                .replace("%role%", roleString)

            sendMsg(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.selfRoleWrapper
            val map = wrapper.selfRoleCache.get(context.guildId).await()

            val language = context.getLanguage()
            val msg = if (map.isNotEmpty()) {
                val title = i18n.getTranslation(language, "$root.title")
                val content = StringBuilder("```ini\n[group]:\n [index] - [emoteji] -> [(roleId, roleName), ...]")

                for ((group, emotejiRoleIdMap) in map.toSortedMap()) {
                    content.append("\n${group}:")
                    var counter = 1
                    for ((emoteji, roleIds) in emotejiRoleIdMap.toSortedMap()) {
                        var roleIdString = ""
                        for (roleId in roleIds) {
                            val role = context.guild.getRoleById(roleId)
                            roleIdString += "(" + (role?.idLong ?: "error") + ", " + (role?.name
                                ?: "error") + ")" + ", "
                        }
                        roleIdString = roleIdString.removeSuffix(", ")

                        content.append("\n ${counter++} - [$emoteji] -> [$roleIdString]")
                    }
                }
                content.append("```")
                title + content
            } else {
                i18n.getTranslation(language, "$root.empty")
            }
            sendMsg(context, msg)
        }
    }
}

suspend fun getSelfRoleGroupByGroupNameN(context: CommandContext, group: String): SelfRoleGroup? {
    val wrapper = context.daoManager.selfRoleGroupWrapper
    return wrapper.selfRoleGroupCache[context.guildId].await().firstOrNull { (groupName) ->
        groupName == group
    }
}

suspend fun getSelfRoleGroupByArgN(context: CommandContext, index: Int): SelfRoleGroup? {
    val group = getStringFromArgsNMessage(context, index, 1, 64)
        ?: return null
    return getSelfRoleGroupByGroupNameN(context, group)
}

suspend fun getSelfRoleGroupByArgNMessage(context: CommandContext, index: Int): SelfRoleGroup? {
    val wrapper = context.daoManager.selfRoleGroupWrapper
    val group = getStringFromArgsNMessage(context, index, 1, 64)
        ?: return null
    val selfRoleGroup = wrapper.selfRoleGroupCache[context.guildId].await().firstOrNull { (groupName) ->
        groupName == group
    }
    if (selfRoleGroup == null) {
        val msg = context.getTranslation("message.unknown.selfrolegroup")
            .replace(PLACEHOLDER_ARG, group)
        sendMsg(context, msg)
    }
    return selfRoleGroup
}