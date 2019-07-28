package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Pattern

class CommandContext(
        private val messageReceivedEvent: MessageReceivedEvent,
        val commandParts: List<String>,
        private val container: Container,
        private val commandList: Set<AbstractCommand>
) : ICommandContext {

    override fun getEvent(): MessageReceivedEvent {
        return messageReceivedEvent
    }

    override fun getGuild(): Guild {
        return messageReceivedEvent.guild
    }

    val usedPrefix: String = commandParts[0]
    val offset: Int = retrieveOffset()
    val args: List<String> = commandParts.drop(2)
    val embedColor: Int = container.settings.embedColor
    val prefix: String = container.settings.prefix
    var commandOrder: List<AbstractCommand> = emptyList()
    val botDevIds: LongArray = container.settings.developerIds
    val daoManager = container.daoManager
    val taskManager = container.taskManager
    val jda = messageReceivedEvent.jda
    val guildId = getGuild().idLong
    val authorId = getAuthor().idLong

    fun getCommands() = commandList
    private fun retrieveOffset(): Int {
        var count = 0
        val pattern = Pattern.compile("<@!?(\\d+)>")
        val matcher = pattern.matcher(commandParts[0])

        while (matcher.find()) {
            if (jda.getUserById(matcher.group(1)) != null) count++
        }

        return count
    }
}