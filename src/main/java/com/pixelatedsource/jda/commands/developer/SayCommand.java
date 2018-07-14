package com.pixelatedsource.jda.commands.developer;

import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SayCommand extends Command {

    public SayCommand() {
        this.commandName = "say";
        this.description = "Makes the bot say stuff";
        this.usage = PREFIX + commandName + " <message>";
        this.category = Category.DEVELOPER;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!event.getArgs().matches("\\s+") && !event.getArgs().equalsIgnoreCase("")) {
            event.reply(event.getArgs());
        } else {
            MessageHelper.sendUsage(this, event);
        }
    }
}
