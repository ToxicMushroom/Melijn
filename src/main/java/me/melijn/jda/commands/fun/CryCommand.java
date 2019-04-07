package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class CryCommand extends Command {


    public CryCommand() {
        this.commandName = "cry";
        this.description = "Shows a crying person [anime]";
        this.usage = PREFIX + commandName + " [user | role]";
        this.category = Category.FUN;
        this.aliases = new String[] {"sad"};
        this.id = 19;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                event.getWebUtils().getImage("cry", image -> event.getMessageHelper().sendFunText("**" + event.getAuthor().getName() + "** is crying", image.getUrl(), event));
            } else if (args.length == 1) {
                User target = event.getHelpers().getUserByArgsN(event, args[0]);
                Role role = event.getHelpers().getRoleByArgs(event, args[0]);
                if (target == null && role == null) {
                    event.reply("Unknown user or role");
                } else if (target != null) {
                    event.getWebUtils().getImage("cry", image ->
                            event.getMessageHelper().sendFunText("**" + target.getName() + "** made **" + event.getAuthor().getName() + "** cry", image.getUrl(), event)
                    );
                } else {
                    event.getWebUtils().getImage("cry", image ->
                            event.getMessageHelper().sendFunText("**" + role.getAsMention() + "** made **" + event.getAuthor().getName() + "** cry", image.getUrl(), event)
                    );
                }
            } else {
                event.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
