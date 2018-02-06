package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class WarnCommand extends Command {

    public WarnCommand() {
        this.commandName = "warn";
        this.description = "warn someone";
        this.usage = PREFIX + commandName + " <user> <reason>";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length >= 2) {
                    User victim = null;
                    if (event.getMessage().getMentionedUsers().size() == 1) {
                        victim = event.getMessage().getMentionedUsers().get(0);
                    } else if (args[0].matches("\\d+") && event.getJDA().retrieveUserById(args[0]).complete() != null) {
                        victim = event.getJDA().retrieveUserById(args[0]).complete();
                    } else {
                        event.reply("Unknown user.");
                        return;
                    }
                    String reason = event.getArgs().replaceFirst(args[0], "");
                    if (PixelSniper.mySQL.addWarn(event.getAuthor(), victim, event.getGuild(), reason)) {
                        event.getMessage().addReaction("\u2705").queue();
                    } else {
                        event.getMessage().addReaction("\u274C").queue();
                    }
                } else {
                    if (event.getGuild() != null) {
                        event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                    } else {
                        event.reply(usage);
                    }
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
