package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.Container
import org.reflections.Reflections
import java.util.*


class CommandClientBuilder(private val container: Container) {

    private val commands = HashSet<AbstractCommand>()

    init {
        container.daoManager.commandWrapper.clearCommands()
    }

    fun build(): CommandClient {
        return CommandClient(commands.toSet(), container)
    }

    private suspend fun addCommand(command: AbstractCommand): CommandClientBuilder {
        commands.add(command)
        container.daoManager.commandWrapper.insert(command)
        return this
    }

    suspend fun loadCommands(): CommandClientBuilder {
        val reflections = Reflections("me.melijn.melijnbot.commands")

        val commands = reflections.getSubTypesOf(AbstractCommand::class.java)
        commands.forEach { command ->
            try {
                if (!command.isMemberClass) {
                    val cmd = command.getDeclaredConstructor().newInstance()
                    addCommand(cmd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }
}