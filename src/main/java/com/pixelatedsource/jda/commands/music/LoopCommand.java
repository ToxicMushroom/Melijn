package com.pixelatedsource.jda.commands.music;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.name = "loop";
        this.help = "Usage: " + PixelSniper.PREFIX + this.name + " <false|true>";
        this.aliases = new String[] {"repeat"};
        this.guildOnly = true;

    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                String ts = PixelSniper.looped.get(event.getGuild()) == null || !PixelSniper.looped.get(event.getGuild()) ? "**disabled**.": "**enabled**.";
                event.reply("Looping is " + ts);
            } else {
                switch (args[0]) {
                    case "yes":
                    case "true":
                        PixelSniper.looped.put(event.getGuild(), true);
                        event.reply("Loop enabled!");
                        break;

                    case "no"://idk if switch works like this
                    case "false":
                        PixelSniper.looped.put(event.getGuild(), false);
                        event.reply("Loop disabled!");
                        break;
                }
            }
        }
    }
}
