package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.Permission;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class TempBan extends Command {

    public TempBan() {
        this.commandName = "tempban";
        this.description = "Ban people and let the bot unban them after the specified amount of days";
        this.usage = PREFIX + commandName + " <@user | userid> <number of days> <reason>";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS))
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length >= 3) {
                    String targetid;
                    String days;
                    String reason;
                    int offset = 0;
                    if (args[0].equalsIgnoreCase("")) offset = 1;
                    if (event.getMessage().getMentionedUsers().size() > 0) targetid = event.getMessage().getMentionedUsers().get(0).getId();
                    else targetid = event.getJDA().retrieveUserById(args[offset]).complete().getId();
                    days = args[1 + offset];
                    reason = event.getArgs().replaceFirst(args[offset], "").replaceFirst(" " + args[offset + 1] + " ", "");
                    if (days.matches("\\d+")) {
                        try {
                            if (PixelSniper.mySQL.setTempBan(event.getJDA(), event.getGuild().getId(), event.getAuthor().getId(), targetid, reason, Long.valueOf(days))) {
                                event.getMessage().addReaction("\u2705").queue();
                            } else {
                                event.getMessage().addReaction("\u274C").queue();
                            }
                        } catch (NumberFormatException ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        event.reply("`" + days + "` is not a number");
                    }
                } else {
                    event.reply(this.usage);
                }
            }
            else event.reply("I don't have permission to ban :(");
        }
    }
}
