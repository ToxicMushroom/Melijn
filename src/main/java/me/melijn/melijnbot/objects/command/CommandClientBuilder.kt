package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.Container
import java.util.*


class CommandClientBuilder(private val container: Container) {

    private val commands = HashSet<ICommand>()


    fun build(): CommandClient {
        return CommandClient(commands, container)
    }

    fun addCommand(command: ICommand): CommandClientBuilder {
        commands.add(command)
        container.daoManager.commandWrapper.insert(command)
        return this
    }
}