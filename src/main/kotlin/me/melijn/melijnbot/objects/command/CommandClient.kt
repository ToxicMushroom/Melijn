package me.melijn.melijnbot.objects.command

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.objects.jagtag.CCJagTagParser
import me.melijn.melijnbot.objects.jagtag.CCJagTagParserArgs
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import java.util.stream.Collectors
import kotlin.random.Random

class CommandClient(private val commandList: Set<AbstractCommand>, private val container: Container) : ListenerAdapter() {

    private val guildPrefixCache = container.daoManager.guildPrefixWrapper.prefixCache
    private val userPrefixCache = container.daoManager.userPrefixWrapper.prefixCache

    private val commandCooldownCache = container.daoManager.commandCooldownWrapper.commandCooldownCache
    private val channelCommandCooldownCache = container.daoManager.commandChannelCoolDownWrapper.commandChannelCooldownCache

    private val disabledCommandCache = container.daoManager.disabledCommandWrapper.disabledCommandsCache
    private val channelCommandStateCache = container.daoManager.channelCommandStateWrapper.channelCommandsStateCache


    private val commandMap: HashMap<String, AbstractCommand> = HashMap()

    init {
        commandList.forEach { command ->
            commandMap[command.name.toLowerCase()] = command
            for (alias in command.aliases) {
                commandMap[alias.toLowerCase()] = command
            }
        }
        container.commandMap = commandList.stream().collect(Collectors.toMap({ cmd: AbstractCommand ->
            cmd.id
        }, { cmd: AbstractCommand ->
            cmd
        }))
    }


    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                commandFinder(event)
            } catch (e: Exception) {
                e.printStackTrace()

                if (event.isFromType(ChannelType.PRIVATE)) {
                    e.sendInGuild(channel = event.privateChannel, author = event.author)
                } else if (event.isFromType(ChannelType.TEXT)) {
                    e.sendInGuild(event.guild, event.textChannel, event.author)
                }
            }
        }
    }

    private suspend fun commandFinder(event: MessageReceivedEvent) {
        val prefixes = getPrefixes(event)
        val message = event.message

        val ccsWithPrefix = mutableListOf<CustomCommand>()
        val ccsWithoutPrefix = mutableListOf<CustomCommand>()
        if (event.isFromGuild) {

            val ccWrapper = container.daoManager.customCommandWrapper
            val ccs = ccWrapper.customCommandCache.get(event.guild.idLong).await()

            for (cc in ccs) {
                if (cc.prefix) {
                    ccsWithPrefix
                } else {
                    ccsWithoutPrefix
                }.add(cc)
            }
        }

        if (event.channelType == ChannelType.TEXT) {
            if (!event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_WRITE)) return
        }
        val ccsWithPrefixMatches = mutableMapOf<CustomCommand, String>()
        val ccsWithoutPrefixMatches = mutableListOf<CustomCommand>()
        for (prefix in prefixes) {


            if (!message.contentRaw.startsWith(prefix)) continue

            val commandParts: ArrayList<String> = ArrayList(message.contentRaw
                .replaceFirst(prefix, "")
                .trim()
                .split(Regex("\\s+")))

            commandParts.add(0, prefix)

            for (cc in ccsWithPrefix) {
                val aliases = cc.aliases
                if (cc.name.equals(commandParts[1], true)) {
                    ccsWithPrefixMatches[cc] = prefix
                } else if (aliases != null) {
                    for (alias in aliases) {
                        if (alias.equals(commandParts[1], true)) {
                            ccsWithPrefixMatches[cc] = prefix
                        }
                    }
                }
            }

            val command = commandMap.getOrElse(commandParts[1].toLowerCase(), { null }) ?: continue
            if (checksFailed(command, event)) return
            command.run(CommandContext(event, commandParts, container, commandList))
            return
        }

        val commandParts = message.contentRaw.split(Regex("\\s+")).toList()

        if (ccsWithPrefixMatches.isNotEmpty()) {
            runCustomCommandByChance(event, commandParts, ccsWithPrefixMatches)
            return
        } else {
            for (cc in ccsWithoutPrefix) {
                val aliases = cc.aliases
                if (commandParts[0].equals(cc.name, true)) {
                    ccsWithoutPrefixMatches.add(cc)
                } else if (aliases != null) {
                    for (alias in aliases) {
                        if (alias.equals(commandParts[0], true)) {
                            ccsWithoutPrefixMatches.add(cc)
                        }
                    }
                }

            }
        }
        if (ccsWithoutPrefixMatches.isNotEmpty()) {
            val mapje: Map<CustomCommand, String?> = ccsWithoutPrefixMatches.map { cc -> cc to null }.toMap()
            runCustomCommandByChance(event, commandParts, mapje)
            return
        }

    }

    private suspend fun runCustomCommandByChance(event: MessageReceivedEvent, commandParts: List<String>, ccs: Map<CustomCommand, String?>) {
        val cc: CustomCommand = if (ccs.keys.size == 1) {
            ccs.keys.first()
        } else {
            getCustomCommandByChance(ccs.keys.toList())
        }

        if (checksFailed(cc, event)) return

        val cParts = commandParts.toMutableList()
        val prefix = ccs[cc]
        if (prefix != null) {
            cParts.add(0, prefix)
        }

        executeCC(cc, event, cParts, prefix != null)
    }

    private suspend fun executeCC(cc: CustomCommand, event: MessageReceivedEvent, commandParts: List<String>, prefix: Boolean) {
        val member = event.member ?: return
        val channel = event.textChannel
        if (!channel.canTalk()) return

        //registering execution
        val pair1 = Pair(channel.idLong, member.idLong)
        val map1 = container.daoManager.commandChannelCoolDownWrapper.executions[pair1]?.toMutableMap()
            ?: mutableMapOf()
        map1["cc." + cc.id] = System.currentTimeMillis()
        container.daoManager.commandChannelCoolDownWrapper.executions[pair1] = map1

        val pair2 = Pair(member.guild.idLong, member.idLong)
        val map2 = container.daoManager.commandChannelCoolDownWrapper.executions[pair2]?.toMutableMap()
            ?: mutableMapOf()
        map2["cc." + cc.id] = System.currentTimeMillis()
        container.daoManager.commandChannelCoolDownWrapper.executions[pair2] = map2


        val regex = ("${commandParts[0]}\\s*" + if (prefix) "${commandParts[1]}\\s*" else "").toRegex()
        val rawArg = event.message.contentRaw.replaceFirst(regex, "")
        val modularMessage = replaceVariablesInCCMessage(member, rawArg, cc)

        val message: Message? = modularMessage.toMessage()
        when {
            message == null -> sendAttachments(channel, modularMessage.attachments)
            modularMessage.attachments.isNotEmpty() -> sendMsgWithAttachments(channel, message, modularMessage.attachments)
            else -> sendMsg(channel, message)
        }
    }


    private suspend fun replaceVariablesInCCMessage(member: Member, rawArg: String, cc: CustomCommand): ModularMessage {
        val modularMessage = cc.content
        val newMessage = ModularMessage()
        val ccArgs = CCJagTagParserArgs(member, rawArg, cc)

        newMessage.messageContent = modularMessage.messageContent?.let {
            CCJagTagParser.parseCCJagTag(ccArgs, it)
        }

        val oldEmbedData = modularMessage.embed?.toData()
            ?.put("type", EmbedType.RICH)
        if (oldEmbedData != null) {
            val newEmbedJSON = CCJagTagParser.parseCCJagTag(ccArgs, oldEmbedData.toString())
            val newEmbedData = DataObject.fromJson(newEmbedJSON)
            val newEmbed = (member.jda as JDAImpl).entityBuilder.createMessageEmbed(newEmbedData)
            newMessage.embed = newEmbed
        }


        val newAttachments = mutableMapOf<String, String>()
        modularMessage.attachments.forEach { (t, u) ->
            val url = CCJagTagParser.parseCCJagTag(ccArgs, t)
            val file = CCJagTagParser.parseCCJagTag(ccArgs, u)
            newAttachments[url] = file
        }
        newMessage.attachments = newAttachments
        return newMessage

    }

    private fun getCustomCommandByChance(ccs: List<CustomCommand>): CustomCommand {
        var range = 0
        for (cc in ccs) {
            range += cc.chance
        }
        val winner = Random.nextInt(range)
        range = 0
        for (cc in ccs) {
            val bool1 = (range <= winner)
            range += cc.chance
            val bool2 = (range <= winner)
            if (bool1 && !bool2) {
                return cc
            }
        }
        throw IllegalArgumentException("random int ($winner) out of range of ccs")
    }

    private suspend fun getPrefixes(event: MessageReceivedEvent): List<String> {
        var prefixes = if (event.isFromGuild) {
            guildPrefixCache.get(event.guild.idLong).await().toMutableList()
        } else {
            mutableListOf()
        }

        //add default prefix if none are set
        if (prefixes.isEmpty()) {
            prefixes = mutableListOf(container.settings.prefix)
        }

        //registering private prefixes
        prefixes.addAll(userPrefixCache.get(event.author.idLong).await())


        //mentioning the bot will always work
        val tags = arrayOf("<@${event.jda.selfUser.idLong}>", "<@!${event.jda.selfUser.idLong}>")
        for (tag in tags) {
            prefixes.add(tag)
        }

        val jdaMention = if (event.isFromGuild) {
            event.guild.selfMember.asMention
        } else {
            event.jda.selfUser.asMention
        }

        if (!prefixes.contains(jdaMention)) {
            prefixes.add(jdaMention)
        }

        return prefixes.toList()
    }

    /**
     * [@return] returns true if the check failed
     *
     * **/
    private suspend fun checksFailed(command: AbstractCommand, event: MessageReceivedEvent): Boolean {
        val cmdId = command.id.toString()
        if (event.isFromGuild && commandIsDisabled(cmdId, event)) {
            return true
        }

        val conditions = mutableListOf<RunCondition>()
        addConditions(conditions, command.commandCategory.runCondition)
        addConditions(conditions, command.runConditions)

        conditions.forEach {
            if (!RunConditionUtil.runConditionCheckPassed(container, it, event, command)) return true
        }

        if (event.isFromGuild) {
            command.discordPermissions.forEach { permission ->
                val botMember = event.guild.selfMember
                var missingPermissionCount = 0
                var missingPermissionMessage = ""

                if (!botMember.hasPermission(event.textChannel, permission)) {
                    missingPermissionMessage += "\n    ⁎ `${permission.toUCSC()}`"
                    missingPermissionCount++
                }

                if (missingPermissionCount > 0) {
                    val language = getLanguage(container.daoManager, event.author.idLong, event.guild.idLong)
                    val more = if (missingPermissionCount > 1) "s" else ""
                    val msg = i18n.getTranslation(language, "message.discordpermission$more.missing")
                        .replace("%permissions%", missingPermissionMessage)
                        .replace("%channel%", event.textChannel.asTag)

                    sendMsg(event.textChannel, msg)
                    return true
                }
            }

            if (commandIsOnCooldown(cmdId, event)) {
                return true
            }
        }

        return false
    }

    private fun addConditions(conditionList: MutableList<RunCondition>, list: Array<RunCondition>) {
        for (runCondition in list) {
            addConditions(conditionList, runCondition.preRequired)
            if (!conditionList.contains(runCondition)) {
                conditionList.add(runCondition)
            }
        }
    }

    /**
     * [@return] returns true if the check failed
     *
     * **/
    private suspend fun checksFailed(command: CustomCommand, event: MessageReceivedEvent): Boolean {
        val cmdId = "cc.${command.id}"
        if (commandIsDisabled(cmdId, event)) {
            return true
        }

        if (commandIsOnCooldown(cmdId, event)) {
            return true
        }

        return false
    }


    private suspend fun commandIsDisabled(id: String, event: MessageReceivedEvent): Boolean {
        val disabledChannelCommands = channelCommandStateCache.get(event.channel.idLong).await()
        if (disabledChannelCommands.contains(id)) {
            if (disabledChannelCommands[id] == ChannelCommandState.ENABLED) {
                return false
            } else if (disabledChannelCommands[id] == ChannelCommandState.DISABLED) {
                return true
            }
        }

        val disabledCommands = disabledCommandCache.get(event.guild.idLong).await()
        if (disabledCommands.contains(id)) return true

        return false
    }

    private suspend fun commandIsOnCooldown(id: String, event: MessageReceivedEvent): Boolean {
        val guildId = event.guild.idLong
        val userId = event.author.idLong
        val channelId = event.channel.idLong

        if (!container.daoManager.commandChannelCoolDownWrapper.executions.contains(Pair(guildId, userId))) {
            return false
        }

        var lastExecution = 0L
        var lastExecutionChannel = 0L
        var bool = false
        var cooldownResult = 0L

        val commandChannelCooldowns = channelCommandCooldownCache.get(channelId).await()
        if (commandChannelCooldowns.containsKey(id)) {

            //init lastExecutionChannel
            container.daoManager.commandChannelCoolDownWrapper.executions[Pair(channelId, userId)]
                ?.filter { entry -> entry.key == id }
                ?.forEach { entry ->
                    if (entry.value > lastExecutionChannel) lastExecutionChannel = entry.value
                }

            val cooldown = commandChannelCooldowns[id] ?: 0L

            if (System.currentTimeMillis() - cooldown < lastExecutionChannel) {
                cooldownResult = cooldown
                bool = true
            }
        }
        val commandCooldowns = commandCooldownCache.get(guildId).await()
        if (commandCooldowns.containsKey(id)) {

            //init lastExecution
            container.daoManager.commandChannelCoolDownWrapper.executions[Pair(guildId, userId)]
                ?.filter { entry -> entry.key == id }
                ?.forEach { entry ->
                    if (entry.value > lastExecution) lastExecution = entry.value
                }

            val cooldown = commandCooldowns[id] ?: 0L

            if (System.currentTimeMillis() - cooldown < lastExecution) {
                if (cooldownResult < cooldown) cooldownResult = cooldown
                bool = true
            }
        }
        val lastExecutionBiggest = if (lastExecution > lastExecutionChannel) {
            lastExecution
        } else {
            lastExecutionChannel
        }

        if (bool && cooldownResult != 0L) {

            val language = getLanguage(container.daoManager, userId, guildId)
            val unReplacedCooldown = i18n.getTranslation(language, "message.cooldown")
            val msg = unReplacedCooldown
                .replace("%cooldown%", ((cooldownResult - (System.currentTimeMillis() - lastExecutionBiggest)) / 1000.0).toString())
            sendMsg(event.textChannel, msg)
        }
        return bool
    }
}