package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.WebUtils;

import static me.melijn.jda.Melijn.PREFIX;

public class ShrugCommand extends Command {

    private WebUtils webUtils;

    public ShrugCommand() {
        this.commandName = "shrug";
        this.description = "Shows a shrugging person [anime]";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"idk"};
        webUtils = WebUtils.getWebUtilsInstance();
        this.id = 20;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            webUtils.getImage("shrug", image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** shrugs ¯\\_(ツ)_/¯", image.getUrl(), event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
