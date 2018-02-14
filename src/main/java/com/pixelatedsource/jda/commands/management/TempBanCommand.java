package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class TempBanCommand extends Command {

    public TempBanCommand() {
        this.commandName = "tempban";
        this.description = "Ban people and let the bot unban them after the specified amount of days";
        this.usage = PREFIX + commandName + " <@user | userid> <time> <reason>";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                if (event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                    String[] args = event.getArgs().split("\\s+");
                    if (args.length >= 3) {
                        User target;
                        String time = args[1];
                        String reason = event.getArgs().replaceFirst(args[0], "").replaceFirst(" " + args[1] + " ", "");
                        if (event.getMessage().getMentionedUsers().size() > 0) target = event.getMessage().getMentionedUsers().get(0);
                        else target = event.getJDA().retrieveUserById(args[0]).complete();
                        if (target == null) {
                            event.reply("Unknown user!");
                            return;
                        }
                        if (MessageHelper.isRightFormat(time)) {
                            try {
                                if (PixelSniper.mySQL.setTempBan(event.getAuthor(), target, event.getGuild(), reason, MessageHelper.easyFormatToSeconds(time))) {
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
                        event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                    }
                } else {
                    event.reply("I have no permission to ban users.");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
