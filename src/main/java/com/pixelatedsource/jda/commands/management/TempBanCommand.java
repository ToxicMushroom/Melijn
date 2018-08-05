package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class TempBanCommand extends Command {

    public TempBanCommand() {
        this.commandName = "tempban";
        this.description = "Ban people and let the bot unban them after the specified amount of time";
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
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 1) {
                Helpers.retrieveUserByArgsN(event, args[0], target -> {
                    String time = args[1];
                    if (target != null) {
                        if (MessageHelper.isRightFormat(time)) {
                            try {
                                if (event.getGuild().getMember(target).getRoles().size() > 1) {
                                    if (event.getGuild().getMember(target).getRoles().get(0).getPosition() <= event.getGuild().getSelfMember().getRoles().get(0).getPosition()) {
                                        event.reply("I can't modify a member with higher or equal highest role than myself");
                                        return;
                                    }
                                }
                                String reason = event.getArgs().replaceFirst(args[0] + "\\s+" + args[1] + "\\s+|" + args[0] + "\\s+" + args[1], "");
                                if (reason.length() <= 1000 && PixelSniper.mySQL.setTempBan(event.getAuthor(), target, event.getGuild(), reason, MessageHelper.easyFormatToSeconds(time))) {
                                    event.getMessage().addReaction("\u2705").queue();
                                } else {
                                    event.getMessage().addReaction("\u274C").queue();
                                }
                            } catch (NumberFormatException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            event.reply("`" + time + "` is not the right format.\n**Format:** (number)(*timeunit*) *timeunit* = s, m, h, d, M or y\n**Example:** 1__m__ (1 __minute__)");
                        }
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
    }
}
