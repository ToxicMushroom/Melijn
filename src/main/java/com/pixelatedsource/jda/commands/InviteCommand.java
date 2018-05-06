package com.pixelatedsource.jda.commands;

import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class InviteCommand extends Command {


    public InviteCommand() {
        this.commandName = "invite";
        this.description = "The bot will give you an awesome link which you have to click";
        this.usage = PREFIX + commandName;
        this.category = Category.DEFAULT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null) {
            event.reply("With permissions included: https://melijn.com/invite?perms=true\n or without https://melijn.com/invite");
        } else {
            event.getAuthor().openPrivateChannel().queue(s -> s.sendMessage("With permissions included: https://melijn.com/invite?perms=true\n or without https://melijn.com/invite").queue());
        }
    }
}
