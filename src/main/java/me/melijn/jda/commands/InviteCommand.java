package me.melijn.jda.commands;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class InviteCommand extends Command {


    public InviteCommand() {
        this.commandName = "invite";
        this.description = "The bot will give you an awesome link which you have to click";
        this.usage = PREFIX + commandName;
        this.category = Category.DEFAULT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null) {
            event.reply("With permissions included: https://melijn.com/invite?perms=true\n or without https://melijn.com/invite");
        } else {
            if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_ADD_REACTION)) event.getMessage().addReaction("\u2705").queue();
            else if (event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_WRITE)) event.reply("Check your dm's");
            event.getAuthor().openPrivateChannel().queue(s -> s.sendMessage("With permissions included: https://melijn.com/invite?perms=true\n or without https://melijn.com/invite").queue());
        }
    }
}
