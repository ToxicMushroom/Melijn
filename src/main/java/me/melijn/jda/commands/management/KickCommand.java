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
import net.dv8tion.jda.core.entities.User;

import static me.melijn.jda.Melijn.PREFIX;

public class KickCommand extends Command {

    public KickCommand() {
        this.commandName = "kick";
        this.description = "Kicks a member from your server and sends a message with information about the kick to that member";
        this.usage = PREFIX + commandName + " <member> [reason]";
        this.category = Category.MANAGEMENT;
        this.extra = "the bot will dm the reason to the target if one is provided";
        this.needs = new Need[]{Need.GUILD, Need.ROLE};
        this.permissions = new Permission[]{Permission.KICK_MEMBERS, Permission.MESSAGE_HISTORY};
        this.id = 52;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].isEmpty()) {
                User target = Helpers.getUserByArgsN(event, args[0]);
                String reason = event.getArgs().replaceFirst(args[0] + "\\s+|" + args[0], "");
                if (target == null) {
                    event.reply("This user isn't a member of this guild");
                    return;
                }
                if (event.getGuild().getMember(target) != null) {
                    TaskScheduler.async(() -> {
                        if (Helpers.canNotInteract(event, target)) return;
                        if (reason.length() <= 1000 && Melijn.mySQL.addKick(event.getAuthor(), target, event.getGuild(), reason)) {
                            event.getMessage().addReaction("\u2705").queue();
                        } else {
                            event.getMessage().addReaction("\u274C").queue();
                        }
                    });

                } else {
                    event.reply("Unknown user");
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
