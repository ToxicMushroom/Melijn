package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu
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
        val menu: SelectionMenu = SelectionMenu.create("menu:class")
            .setPlaceholder("Choose your class") // shows the placeholder indicating what this menu is for
            .setRequiredRange(1, 2) // only one can be selected
            .addOption("mage-arcane", "Arcane Mage")
            .addOption("mage-fire", "Fire Mage")
            .addOption("mage-frost", "Frost Mage")
            .build()

        val msgId = context.channel.sendMessage(MessageBuilder().apply {
            setContent("sex")
            setActionRows(
                ActionRow.of(
                    menu
                )
            )
        }.build()).await().idLong

        context.container.eventWaiter.waitFor(SelectionMenuEvent::class.java, { event ->
            event.selectionMenu?.id == "menu:class" &&
                event.user.idLong == context.authorId &&
                event.messageIdLong == msgId
        }, { event ->
            event.reply(
                event.selectedOptions?.joinToString("\n") { "${it.emoji ?: ""}${it.label}, ${it.value}, ${it.description ?: "/"}, ${it.isDefault}" }
                    ?: ""
            ).queue()
        }, {
            sendRsp(context, "You should click the thing")
        }, 20)
    }
}
