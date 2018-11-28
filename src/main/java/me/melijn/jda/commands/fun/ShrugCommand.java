package me.melijn.jda.commands.fun;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.CrapUtils;

import static me.melijn.jda.Melijn.PREFIX;

public class ShrugCommand extends Command {

    private CrapUtils crapUtils;

    public ShrugCommand() {
        this.commandName = "shrug";
        this.description = "shrug";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"idk"};
        crapUtils = CrapUtils.getWebUtilsInstance();
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            crapUtils.getImage("shrug", image -> MessageHelper.sendFunText("**" + event.getAuthor().getName() + "** shrugs ¯\\_(ツ)_/¯", image.getUrl(), event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
