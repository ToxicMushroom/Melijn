package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;

public class LoopCommand extends Command {

    public LoopCommand() {
        this.name = "loop";
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name + " <false|true>";
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean acces = false;
        if (event.getGuild() == null) acces = true;
        if (!acces) acces = Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name);
        if (acces) {
            String[] args = event.getArgs().split("\\s+");
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
