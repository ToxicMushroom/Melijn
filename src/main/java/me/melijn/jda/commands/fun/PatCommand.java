package me.melijn.jda.commands.fun;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class PatCommand extends Command {

    public PatCommand() {
        this.commandName = "pat";
        this.description = "Shows a person being patted [anime]";
        this.usage = PREFIX + commandName + " [user]";
        this.category = Category.FUN;
        this.id = 41;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() == null || event.hasPerm(event.getMember(), commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].isEmpty()) {
                event.getWebUtils().getImage("pat",
                        image -> event.getMessageHelper().sendFunText("**" + event.getBotName() + "** patted you", image.getUrl(), event)
                );
            } else if (args.length == 1) {
                User target = event.getHelpers().getUserByArgsN(event, args[0]);
                if (target == null) {
                    event.reply("Didn't catch that? Try harder");
                } else {
                    event.getWebUtils().getImage("pat",
                            image -> event.getMessageHelper().sendFunText("**" + event.getAuthor().getName() + "** patted **" + target.getName() + "**", image.getUrl(), event)
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
