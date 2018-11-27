package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class UnbanCommand extends Command {

    public UnbanCommand() {
        this.commandName = "unban";
        this.description = "Unbans a banned user";
        this.usage = PREFIX + commandName + " <user>";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.BAN_MEMBERS,
                Permission.MESSAGE_HISTORY
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isBlank()) {
                Helpers.retrieveUserByArgsN(event, args[0], user -> {
                    if (user == null) {
                        event.reply("Unknown user");
                        return;
                    }
                    TaskScheduler.async(() -> {
                        String reason = args.length > 1 ? event.getArgs().replaceFirst(args[0], "") : "N/A";
                        if (reason.substring(0, 1).equalsIgnoreCase(" ")) reason = reason.replaceFirst("\\s+", "");
                        if (Melijn.mySQL.unban(user, event.getGuild(), event.getAuthor(), reason)) {
                            event.getMessage().addReaction("\u2705").queue();
                        } else {
                            event.getMessage().addReaction("\u274C").queue();
                        }
                    });
                });
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
