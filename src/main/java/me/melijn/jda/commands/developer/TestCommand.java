package me.melijn.jda.commands.developer;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class TestCommand extends Command {

    public TestCommand() {
        this.commandName = "test";
        this.description = "this command is for testing";
        this.usage = PREFIX + commandName;
        this.category = Category.DEVELOPER;
        this.id = 104;
    }

    @Override
    protected void execute(CommandEvent event) {
        event.reply("Repeats: " + event.getVariables().timerAmount + "\nQueries: " + event.getVariables().queryAmount);
    }
}
