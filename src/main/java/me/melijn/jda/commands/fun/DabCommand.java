package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class DabCommand extends Command {

    public DabCommand() {
        this.commandName = "dab";
        this.description = "Shows a dabbing person [anime]";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.id = 21;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            event.getWebUtils().getImage("dab", image -> event.getMessageHelper().sendFunText("**" + event.getAuthor().getName() + "** is dabbing", image.getUrl(), event));
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
