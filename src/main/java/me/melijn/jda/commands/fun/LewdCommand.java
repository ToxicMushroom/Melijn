package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;

import static me.melijn.jda.Melijn.PREFIX;

public class LewdCommand extends Command {

    public LewdCommand() {
        this.commandName = "lewd";
        this.description = "Shows a lewd person [anime]";
        this.usage = PREFIX + commandName;
        this.category = Category.FUN;
        this.id = 24;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            event.getWebUtils().getImage("lewd",
                    image -> event.getMessageHelper().sendFunText("**" + event.getAuthor().getName() + "** is being lewd", image.getUrl(), event)
            );
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
