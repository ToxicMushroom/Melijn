package me.melijn.melijnbot.objects.command

import me.duncte123.botcommons.messaging.MessageUtils
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.utils.toUpperWordCase
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandClient(val commandList: Set<ICommand>, val container: Container) : ListenerAdapter() {

    val guildPrefixCache = container.daoManager.guildPrefixWrapper.prefixCache
    //val userPrefixCache = container.daoManager.userPrefixWrapper.prefixCache

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return

        val author = event.author
        val message = event.message
        var prefix = container.settings.prefix

        container.taskManager.async(Runnable {
            if (event.isFromGuild) {
                val guild = event.guild
                val prefixesFuture = guildPrefixCache.get(guild.idLong)
                val prefixes = prefixesFuture.get()

                prefixes.forEach { pr ->
                    if (message.contentRaw.startsWith(pr)) prefix = pr
                }
            }

            val messageParts: List<String> = message.contentRaw.replaceFirst(prefix, "").split("\\s+")
            val possibleCommandName = messageParts[0]

            commandList.forEach { command ->
                if (command.isCommandFor(possibleCommandName)) {
                    if (checksFailed(command, event)) return@Runnable
                    command.run(CommandContext(event, container, commandList))
                }
            }
        })
    }

    fun checksFailed(command: ICommand, event: MessageReceivedEvent): Boolean {
        command.runConditions.forEach {
            if (!runConditionCheckPassed(it, event)) return true
        }

        if (event.isFromGuild) {
            command.discordPermissions.forEach { permission ->
                val botMember = event.guild.selfMember
                var missesPermission = false
                var missingPermissionMessage = ""
                if (!botMember.hasPermission(event.textChannel, permission)) {
                    missingPermissionMessage += "\n ⁎**${permission.toString().toUpperWordCase()}**"
                    missesPermission = true
                }

                if (missesPermission) {
                    missingPermissionMessage = "I'm missing the following permission" +
                            (if (missingPermissionMessage.count { c -> c == '\n' } > 1) "s" else "") +
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
    fun runConditionCheckPassed(runCondition: RunCondition, event: MessageReceivedEvent): Boolean {
        return when (runCondition) {
            RunCondition.GUILD -> event.isFromGuild
            RunCondition.VC_BOT_ALONE_OR_USER_DJ -> false
            else -> false
        }
    }


}