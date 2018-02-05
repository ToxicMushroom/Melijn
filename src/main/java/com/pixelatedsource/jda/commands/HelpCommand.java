package com.pixelatedsource.jda.commands;

import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class HelpCommand extends Command {

    public HelpCommand() {
        this.commandName = "help";
        this.description = "Shows you this menu";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"commands", "cmds"};
        this.category = Category.DEFAULT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getAuthor().isFake()) return;
        event.getAuthor().openPrivateChannel().queue(c -> {
            c.sendMessage("You can find help on my fancy webpage https://pixelnetwork.be/commands").queue();
            if (event.getGuild() != null) event.getMessage().addReaction("\u2705").queue();
        });
    }
}
