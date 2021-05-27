package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Emoji
import net.dv8tion.jda.api.interactions.ActionRow
import net.dv8tion.jda.api.interactions.button.Button
import net.dv8tion.jda.api.interactions.button.ButtonStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    val logger: Logger = LoggerFactory.getLogger(TestCommand::class.java)

    override suspend fun execute(context: ICommandContext) {
        context.textChannel.sendMessage(
            MessageBuilder()
                .setContent("hi")
                .setActionRows(ActionRow.of(
                    Button.success("id", "hi"),
                    Button.link("https://melijn.com", "blub"),
                    Button.of(ButtonStyle.PRIMARY, "uwu", Emoji.ofUnicode("❤"))
                )).build()
        ).queue()

    }
}