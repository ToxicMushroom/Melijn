package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;

public class UnmuteCommand extends Command {

    public UnmuteCommand() {
        this.commandName = "unmute";
        this.description = "unmute a muted user";
        this.usage = Melijn.PREFIX + commandName + " <user> [reason]";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MANAGE_ROLES,
                Permission.MESSAGE_HISTORY
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].isBlank()) {
                    Helpers.retrieveUserByArgsN(event, args[0], user -> {
                        if (user != null) {
                            TaskScheduler.async(() -> {
                                String reason = event.getArgs().replaceFirst(args[0], "");
                                if (reason.length() == 0 || reason.matches("\\s+")) reason = "N/A";
                                if (reason.startsWith(" ")) reason = reason.replaceFirst("\\s+", "");
                                if (Melijn.mySQL.unmute(event.getGuild(), user, event.getAuthor(), reason)) {
                                    event.getMessage().addReaction("\u2705").queue();
                                } else {
                                    event.getMessage().addReaction("\u274C").queue();
                                }
                            });
                        } else {
                            event.reply("Unknown user");
                        }
                    });
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
