package me.melijn.melijnbot.objects.command

import me.duncte123.botcommons.messaging.MessageUtils
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.utils.toUpperWordCase
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandClient(val commandList: Set<AbstractCommand>, val container: Container) : ListenerAdapter() {

    val guildPrefixCache = container.daoManager.guildPrefixWrapper.prefixCache
    val userPrefixCache = container.daoManager.userPrefixWrapper.prefixCache
    val commandMap: HashMap<String, AbstractCommand> = HashMap()

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