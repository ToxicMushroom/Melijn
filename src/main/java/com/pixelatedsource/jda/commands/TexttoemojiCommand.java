package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;

public class TexttoemojiCommand extends Command {

    public TexttoemojiCommand() {
        this.name = "t2e";
        this.help = "Converts your text into text emotes ;D";
    }

    @Override
    protected void execute(CommandEvent event) {
        StringBuilder sb = new StringBuilder();
        for (String s : event.getArgs().split("")) {
            if (Character.isLetter(s.toLowerCase().charAt(0))) {
                sb.append(":regional_indicator_").append(s.toLowerCase()).append(":");
            } else if (Character.isDigit(s.charAt(0))) {
                sb.append(":").append(Helpers.numberToString(Integer.valueOf(s))).append(":");
            } else {
                if (" ".equals(s)) sb.append(" ");
                sb.append(s);
            }
        }
        event.reply(sb.toString());
    }
}
