package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class HighfiveCommand extends Command {

    public HighfiveCommand() {
        this.commandName = "highfive";
        this.description = "Shows a highfiving person [anime]";
        this.usage = PREFIX + commandName + " [user | role]";
        this.category = Category.FUN;
        this.id = 22;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                event.getWebUtils().getImage("highfive", image -> event.getMessageHelper().sendFunText("**" + event.getBotName() + "** highfived you", image.getUrl(), event));
            } else if (args.length == 1) {
                User target = event.getHelpers().getUserByArgsN(event, args[0]);
                Role role = event.getHelpers().getRoleByArgs(event, args[0]);
                if (target == null && role == null) {
                    event.reply("Unknown user or role");
                } else if (target != null) {
                    event.getWebUtils().getImage("highfive", image ->
                            event.getMessageHelper().sendFunText("**" + event.getAuthor().getName() + "** highfived **" + target.getName() + "**", image.getUrl(), event)
                    );
                } else {
                    event.getWebUtils().getImage("highfive", image ->
                            event.getMessageHelper().sendFunText("**" + event.getAuthor().getName() + "** highfived **" + role.getAsMention() + "**", image.getUrl(), event)
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
