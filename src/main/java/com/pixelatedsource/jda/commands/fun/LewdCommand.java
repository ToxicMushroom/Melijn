package com.pixelatedsource.jda.commands.fun;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import com.pixelatedsource.jda.utils.WebUtils;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class LewdCommand extends Command {

    public LewdCommand() {
        this.commandName = "lewd";
        this.description = "Shows a lewd image";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    private WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            webUtils.getImage("lewd",
                    image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** is being lewd", image.getUrl(), event)
            );
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
