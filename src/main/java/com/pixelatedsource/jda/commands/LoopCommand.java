package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.name = "loop";
        this.help = "Enable or disable song looping";
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split(" ");
        if (args[args.length-1].equalsIgnoreCase("true"))
        {PixelatedBot.looped.put(event.getGuild(), true);
            Helpers.DefaultEmbed("Loop","Enabled",event.getTextChannel());
        }
        else {PixelatedBot.looped.put(event.getGuild(), false);
            Helpers.DefaultEmbed("Loop","Disabled",event.getTextChannel());
        }
    }
}
