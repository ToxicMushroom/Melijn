package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class DogCommand extends Command {

    public DogCommand() {
        this.commandName = "dog";
        this.description = "Shows you a random dog";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"hond"};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            event.reply(WebUtils.getDogUrl());
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
