package me.melijn.melijnbot.internals.command

import dev.minn.jda.ktx.SLF4J
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.SuspendListener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.build.CommandData

class SlashCommandClient(container: Container) : SuspendListener() {

    val logger by SLF4J
    val config = "commands.xml"
    val executor: CommandExecutor<SlashCommandContext, CommandFailure>
    val commands: List<CommandData>

    init {
        // Load commands
        val stream = this.javaClass.getResourceAsStream("/$config") ?: throw IllegalStateException("Missing $config")
        val handler = XMLCommandLoader.load(stream)
        logger.info("Loaded ${handler.commandCount} commands")

        executor = handler.createExecutor("me.melijn.melijnbot.commands")
        commands = handler.buildCommands()
        container.slashCommands.addAll(commands.toSet())
    }

    override suspend fun onEvent(event: GenericEvent) {
        if (event is SlashCommandEvent) {
            onSlashCommand(event)
        }
    }

    private suspend fun onSlashCommand(event: SlashCommandEvent) {
        executor.execute(SlashCommandContext(event))
    }
}