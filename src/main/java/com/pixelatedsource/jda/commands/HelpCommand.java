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
        if (event.getGuild() != null) {
            event.reply("https://www.pixelnetwork.be/commands/index.php?id=" + event.getGuild().getId());
        } else {
            event.reply("https://www.pixelnetwork.be/commands/");
        }
    }
}
