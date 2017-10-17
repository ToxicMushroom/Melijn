package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;

public class TexttoemojiCommand extends Command {

    public TexttoemojiCommand() {
        this.name = "t2e";
        this.help = "Converts your text into text emotes ;D";
    }

    @Override
    protected void execute(CommandEvent event) {
        event.reply(event.getMessage().getRawContent().replaceAll("(.{1})", ":regional_indicator_$1:"));
    }
}
