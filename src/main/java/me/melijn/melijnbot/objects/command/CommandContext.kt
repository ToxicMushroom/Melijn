package me.melijn.melijnbot.objects.command

import me.duncte123.botcommons.commands.ICommandContext
import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class CommandContext(
        private val messageReceivedEvent: MessageReceivedEvent,
        var commandOrder: List<AbstractCommand>,
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


    val daoManager = container.daoManager
    val taskManager = container.taskManager
    val messageUtils = container.messageUtils
    val jda = messageReceivedEvent.jda

    fun getCommands() = commandList
}