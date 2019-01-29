package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class ShrugCommand extends Command {


    public ShrugCommand() {
        this.commandName = "shrug";
        this.description = "Shows a shrugging person [anime]";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.aliases = new String[]{"idk"};
        this.id = 20;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            event.getWebUtils().getImage("shrug", image -> event.getMessageHelper().sendFunText("**" + event.getAuthor().getName() + "** shrugs ¯\\_(ツ)_/¯", image.getUrl(), event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
