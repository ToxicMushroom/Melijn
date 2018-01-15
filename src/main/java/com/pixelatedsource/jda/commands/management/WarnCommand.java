package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
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
                    } else if (args[0].matches("\\d+")) {

                    }
                } else {
                    event.reply(this.usage);
                }
            }
        }
    }
}
