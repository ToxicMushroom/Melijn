package me.melijn.melijnbot.objects.command

import me.duncte123.botcommons.messaging.MessageUtils
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelCommandState
import me.melijn.melijnbot.objects.utils.toUpperWordCase
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

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
            commandMap[command.name] = command
            for (alias in command.aliases) {
                commandMap[alias] = command
            }
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val author = event.author
        val message = event.message

        container.taskManager.async(Runnable {
            if (event.isFromGuild) {
                val guild = event.guild
                val prefixesFuture = guildPrefixCache.get(guild.idLong)
                var prefixes = prefixesFuture.get()
                if (prefixes.isEmpty()) prefixes = listOf(container.settings.prefix)
                if (container.daoManager.supporterWrapper.supporterIds.contains(author.idLong))
                    prefixes = prefixes + userPrefixCache.get(author.idLong).get()

                for (prefix in prefixes) {
                    if (message.contentRaw.startsWith(prefix)) {
                        val commandParts: ArrayList<String> = ArrayList(message.contentRaw
                                .replaceFirst(Regex("$prefix($:\\s+)?"), "")
                                .split(Regex("\\s+")))
                        commandParts.add(0, prefix)

                        val command = commandMap.getOrElse(commandParts[1], { null }) ?: continue
                        if (checksFailed(command, event)) return@Runnable
                        command.run(CommandContext(event, commandParts, container, commandList))
                        break
                    }
                }
            }
        })
    }

    /**
     * [@return] returns true if the check failed
     *
     * **/
    private fun checksFailed(command: AbstractCommand, event: MessageReceivedEvent): Boolean {
        if (event.isFromGuild) {
            if (commandIsDisabled(command, event)) return true
        }

        command.runConditions.forEach {
            if (!runConditionCheckPassed(it, event)) return true
        }

        if (event.isFromGuild) {
            command.discordPermissions.forEach { permission ->
                val botMember = event.guild.selfMember
                var missingPermissionCount = 0
                var missingPermissionMessage = ""
                if (!botMember.hasPermission(event.textChannel, permission)) {
                    missingPermissionMessage += "\n ⁎**${permission.toString().toUpperWordCase()}**"
                    missingPermissionCount++
                }

                if (missingPermissionCount > 0) {
                    missingPermissionMessage =
                            "I'm missing the following permission" +
                                    (if (missingPermissionCount > 1) "s" else "") +
                                    missingPermissionMessage

                    MessageUtils.sendMsg(event, missingPermissionMessage)
                    return true
                }
            }

            if (commandIsOnCooldown(command, event)) return true

        }

        return false
    }

    private fun commandIsDisabled(command: AbstractCommand, event: MessageReceivedEvent): Boolean {
        val disabledChannelCommands = channelCommandStateCache.get(event.channel.idLong).get()
        if (disabledChannelCommands.contains(command.id)) {
            when (disabledChannelCommands[command.id]) {
                ChannelCommandState.ENABLED -> return false
                ChannelCommandState.DISABLED -> return true
            }
        }

        val disabledCommands = disabledCommandCache.get(event.guild.idLong).get()
        if (disabledCommands.contains(command.id)) return true

        return false
    }

    private fun commandIsOnCooldown(command: AbstractCommand, event: MessageReceivedEvent): Boolean {
        val guildId = event.guild.idLong
        val userId = event.author.idLong
        val channelId = event.channel.idLong

        if (!container.daoManager.commandChannelCoolDownWrapper.executions.contains(Pair(guildId, userId))) return false

        var lastExecution = 0L
        var lastExecutionChannel = 0L

        if (channelCommandCooldownCache.get(channelId).get().containsKey(command.id)) {

            //init lastExecutionChannel
            container.daoManager.commandChannelCoolDownWrapper.executions[Pair(guildId, userId)]
                    ?.filter { pair -> pair.first == channelId }
                    ?.forEach { fPair ->
                        if (fPair.second > lastExecutionChannel) lastExecutionChannel = fPair.second
                    }

            val cooldown = channelCommandCooldownCache.get(guildId).get()[command.id] ?: 0L

            if (System.currentTimeMillis() - cooldown < lastExecutionChannel) return true
        }
        if (commandCooldownCache.get(guildId).get().containsKey(command.id)) {

            //init lastExecution
            container.daoManager.commandChannelCoolDownWrapper.executions[Pair(guildId, userId)]
                    ?.forEach { pair ->
                        if (pair.second > lastExecution) lastExecution = pair.second
                    }

            val cooldown = commandCooldownCache.get(guildId).get()[command.id] ?: 0L

            if (System.currentTimeMillis() - cooldown < lastExecutionChannel) return true
        }
        return false
    }

    /**
     * [@return] returns true if the check passed
     *
     * **/
    private fun runConditionCheckPassed(runCondition: RunCondition, event: MessageReceivedEvent): Boolean {
        return when (runCondition) {
            RunCondition.GUILD -> event.isFromGuild
            RunCondition.VC_BOT_ALONE_OR_USER_DJ -> false
        }
    }


}