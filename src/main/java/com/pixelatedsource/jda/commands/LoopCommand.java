package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.name = "loop";
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name + " <false|true>";
        this.aliases = new String[] {"repeat"};
        this.guildOnly = true;

    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                String ts = PixelatedBot.looped.get(event.getGuild()) == null || !PixelatedBot.looped.get(event.getGuild()) ? "**disabled**.": "**enabled**.";
                event.reply("Looping is " + ts);
            } else {
                switch (args[0]) {
                    case "yes":
                    case "true":
                        PixelatedBot.looped.put(event.getGuild(), true);
                        event.reply("Loop enabled!");
                        break;

                    case "no"://idk if switch works like this
                    case "false":
                        PixelatedBot.looped.put(event.getGuild(), false);
                        event.reply("Loop disabled!");
                        break;
                }
            }
        }
    }
}
