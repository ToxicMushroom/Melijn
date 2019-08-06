package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.Container
import java.util.*


class CommandClientBuilder(private val container: Container) {

    private val commands = HashSet<AbstractCommand>()

    init {
        container.daoManager.commandWrapper.clearCommands()
    }

    fun build(): CommandClient {
        return CommandClient(commands.toSet(), container)
    }

    fun addCommand(command: AbstractCommand): CommandClientBuilder {
        commands.add(command)
        container.daoManager.commandWrapper.insert(command)
        return this
    }
}