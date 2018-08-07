package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;

import static me.melijn.jda.Melijn.PREFIX;

public class DabCommand extends Command {

    public DabCommand() {
        this.commandName = "dab";
        this.description = "dab on them haters";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        webUtils = WebUtils.getWebUtilsInstance();
    }

    WebUtils webUtils;

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            webUtils.getImage("dab", image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** is dabbing", image.getUrl() , event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
