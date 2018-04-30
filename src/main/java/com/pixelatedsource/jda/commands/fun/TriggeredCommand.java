package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.WebUtils;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class TriggeredCommand extends Command {

    public TriggeredCommand() {
        this.commandName = "triggered";
        this.description = "Will visualize your triggered state to other people";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"rage"};
        this.category = Category.FUN;
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0);
        if (acces) {
            event.reply(WebUtils.getUrl("triggered"));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
