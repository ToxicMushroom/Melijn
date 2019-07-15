package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandClient(val commandList: Set<ICommand>, val container: Container) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (!event.message.contentRaw.startsWith(container.settings.prefix)) return
        val messageParts: List<String> = event.message.contentRaw.split("\\s+")
        val possibleCommandName = messageParts[0].replace(container.settings.prefix, "")
        commandList.forEach { command ->
            if (command.isCommandFor(possibleCommandName)) {
                command.run(CommandContext(event, container, commandList))
            }
        }
    }
}