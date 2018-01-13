package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;

import java.util.Arrays;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetPrefixCommand extends Command {

    public SetPrefixCommand() {
        this.commandName = "setprefix";
        this.description = "Change the prefix for the commands for your guild";
        this.usage = PREFIX + this.commandName;
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length == 0 || args[0].equalsIgnoreCase("")) event.reply(PixelSniper.mySQL.getPrefix(event.getGuild().getId()));
                else if (Arrays.toString(args).length() <= 100) {
                    if (PixelSniper.mySQL.setPrefix(event.getGuild().getId(), args[0])) {
                        event.reply("The prefix has been set to `" + args[0] + "`");
                    }
                } else {
                    event.reply("The maximum prefix size is 100 characters");
                }
            }
        }
    }
}
