package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.Permission;

import static me.melijn.jda.Melijn.PREFIX;

public class TempBanCommand extends Command {

    public TempBanCommand() {
        this.commandName = "tempban";
        this.description = "Temporally bans a user from your server and sends a message with information about the ban to that user";
        this.usage = PREFIX + commandName + " <user> <time> [reason]";
        this.extra = "Time examples: [1s = 1second, 1m = 1minute, 1h = 1hour, 1w = 1week, 1M = 1month, 1y = 1year]";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.BAN_MEMBERS,
                Permission.MESSAGE_HISTORY
        };
        this.needs = new Need[]{
                Need.GUILD
        };
        this.id = 81;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 1) {
                event.getHelpers().retrieveUserByArgsN(event, args[0], target -> {
                    String time = args[1];
                    if (target == null) {
                        event.reply("Unknown user");
                        return;
                    }
                    if (event.getMessageHelper().isWrongFormat(time)) {
                        event.reply("`" + time + "` is not the right format.\n**Format:** (number)(*timeunit*) *timeunit* = s, m, h, d, M or y\n**Example:** 1__m__ (1 __minute__)");
                        return;
                    }

                    if (event.getGuild().isMember(target) && event.getHelpers().canNotInteract(event, target)) return;
                    String reason = event.getArgs().replaceFirst(args[0] + "\\s+" + args[1] + "\\s+|" + args[0] + "\\s+" + args[1], "");
                    if (reason.length() <= 1000 && event.getMySQL().setTempBan(event.getAuthor(), target, event.getGuild(), reason, event.getMessageHelper().easyFormatToSeconds(time))) {
                        event.getMessage().addReaction("\u2705").queue();
                    } else {
                        event.getMessage().addReaction("\u274C").queue();
                    }

                });
            } else {
                event.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
