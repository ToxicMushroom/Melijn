package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.entities.User;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class UnmuteCommand extends Command {

    public UnmuteCommand() {
        this.commandName = "unmute";
        this.description = "unmute a muted user";
        this.usage = PREFIX + commandName + " <@user | userId>";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 1) {
                    User toUnban;
                    if (event.getMessage().getMentionedUsers().size() == 1) {
                        toUnban = event.getMessage().getMentionedUsers().get(0);
                    } else if (args[0].matches("\\d+") && event.getJDA().retrieveUserById(args[0]).complete() != null) {
                        toUnban = event.getJDA().retrieveUserById(args[0]).complete();
                    } else {
                        event.reply("Unknown user");
                        return;
                    }
                    if (PixelSniper.mySQL.unmute(toUnban, event.getGuild(), event.getJDA())) {
                        event.getMessage().addReaction("\u2705").queue();
                    } else {
                        event.getMessage().addReaction("\u274C").queue();
                    }
                } else {
                    event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
